package app.localbrief.data

import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class Repository(val database: BriefDatabase, private val feeds: FeedProvider = FeedClient()) {
    val dao = database.dao()
    private val lock = Mutex()
    suspend fun prune(now: Long = System.currentTimeMillis()) = dao.prune(Rules.cutoff(now))
    suspend fun setting(key: String, value: String) = dao.putSetting(Setting(key, value))
    suspend fun save(item: Item, savedAt: Long?) = dao.save(item.id, savedAt)
    suspend fun topic(name: String, include: String, exclude: String, sourceIds: Set<Long>, id: Long = 0) = lock.withLock {
        require(name.trim().isNotEmpty()) { "Enter a topic name." }
        database.withTransaction {
            val value = Topic(id, name.trim().take(100), include.trim().take(500), exclude.trim().take(500))
            val topicId = if (id == 0L) dao.addTopic(value) else { dao.updateTopic(value); id }
            dao.unlinkTopic(topicId)
            val existing = dao.allSources().map { it.id }.toSet()
            sourceIds.intersect(existing).forEach { dao.link(SourceTopic(it, topicId)) }
        }
    }
    suspend fun removeTopic(id: Long) = lock.withLock {
        database.withTransaction { dao.unlinkTopic(id); dao.deleteTopic(id) }
    }
    suspend fun addSource(input: String, name: String, topics: Set<Long>): Long = lock.withLock {
        val feed = feeds.discover(input)
        database.withTransaction {
            val kind = if (feed.kind == "YouTube channel") feed.kind else if (runCatching { Rules.normalizeUrl(input) }.getOrNull() != feed.url) "Website feed" else "RSS feed"
            val id = dao.addSource(Source(name = name.trim().ifEmpty { feed.name }.take(200), feedUrl = feed.url, kind = kind))
            topics.intersect(dao.allTopics().map { it.id }.toSet()).forEach { dao.link(SourceTopic(id, it)) }
            val source = dao.allSources().first { it.id == id }
            storeFeed(source, feed)
            id
        }
    }
    suspend fun editSource(source: Source, name: String, topicIds: Set<Long>) = lock.withLock {
        require(name.trim().isNotBlank()) { "Enter a source name." }
        database.withTransaction {
            dao.updateSource(source.copy(name = name.trim().take(200)))
            dao.renameSourceItems(source.id, name.trim().take(200))
            dao.unlinkSource(source.id)
            topicIds.intersect(dao.allTopics().map { it.id }.toSet()).forEach { dao.link(SourceTopic(source.id, it)) }
        }
    }
    suspend fun removeSource(id: Long) = lock.withLock {
        database.withTransaction {
            dao.deleteSourceItems(id); dao.detachSaved(id); dao.deleteSeen(id); dao.unlinkSource(id); dao.deleteSource(id)
        }
    }
    private suspend fun storeFeed(source: Source, feed: Feed) {
        val now = System.currentTimeMillis()
        val seen = dao.seen(source.id).associateBy { it.url }
        feed.entries.forEach { entry ->
            val arrived = seen[entry.url]?.firstSeen ?: now
            dao.remember(SeenEntry(source.id, entry.url, arrived))
            val savedAt = dao.itemByUrl(entry.url)?.savedAt
            if (arrived < Rules.cutoff(now) && savedAt == null) return@forEach
            dao.addItem(Item(sourceId = source.id, sourceName = source.name, url = entry.url,
                title = entry.title, preview = entry.preview, publishedAt = entry.publishedAt,
                firstSeen = arrived, thumbnail = entry.thumbnail, video = entry.video, savedAt = savedAt))
        }
        dao.trimSeen(source.id, feed.entries.map { it.url })
        dao.updateSource(source.copy(lastChecked = now, error = null))
    }
    suspend fun refresh(): Int = lock.withLock {
        var errors = 0
        dao.allSources().forEach { source ->
            try {
                val feed = feeds.fetch(source.feedUrl)
                database.withTransaction { storeFeed(source, feed) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                errors++
                dao.updateSource(source.copy(error = e.message?.take(200) ?: "Could not refresh this source."))
            }
        }
        prune()
        errors
    }
    suspend fun receiveLink(text: String, title: String?): Item = lock.withLock {
        val url = Rules.sharedLink(text) ?: error("No HTTPS link found in the shared text.")
        val now = System.currentTimeMillis()
        database.withTransaction {
            val existing = dao.itemByUrl(url)
            if (existing != null) { dao.save(existing.id, now); existing.copy(savedAt = now) }
            else {
                val item = Item(sourceName = java.net.URI(url).host, url = url,
                    title = title?.takeIf { it.isNotBlank() && !it.startsWith("https://") }?.take(500) ?: url,
                    firstSeen = now, video = "youtube.com/" in url || "youtu.be/" in url, savedAt = now)
                item.copy(id = dao.addItem(item))
            }
        }
    }
    suspend fun exportBackup(): String = lock.withLock { withContext(Dispatchers.IO) {
        database.withTransaction {
            JSONObject().apply {
                put("format", "localbrief"); put("version", 1)
                put("exportedAt", System.currentTimeMillis())
                put("sources", JSONArray().apply { dao.allSources().forEach { s -> put(JSONObject().apply {
                    put("id", s.id); put("name", s.name); put("feedUrl", s.feedUrl); put("kind", s.kind)
                }) } })
                put("topics", JSONArray().apply { dao.allTopics().forEach { t -> put(JSONObject().apply {
                    put("id", t.id); put("name", t.name); put("include", t.includeWords); put("exclude", t.excludeWords)
                }) } })
                put("links", JSONArray().apply { dao.allLinks().forEach { l -> put(JSONObject().apply {
                    put("sourceId", l.sourceId); put("topicId", l.topicId)
                }) } })
                put("saved", JSONArray().apply { dao.allSaved().distinctBy { it.url }.forEach { i -> put(JSONObject().apply {
                    put("sourceId", i.sourceId ?: JSONObject.NULL); put("sourceName", i.sourceName)
                    put("url", i.url); put("title", i.title); put("preview", i.preview)
                    put("publishedAt", i.publishedAt ?: JSONObject.NULL); put("firstSeen", i.firstSeen)
                    put("thumbnail", i.thumbnail ?: JSONObject.NULL); put("video", i.video); put("savedAt", i.savedAt)
                }) } })
                put("settings", JSONObject().apply { dao.allSettings().filter { it.key in BACKUP_SETTINGS }.forEach { put(it.key, it.value) } })
            }.toString(2)
        }
    } }
    // Validate completely before entering the replacement transaction.
    suspend fun restoreBackup(text: String) = lock.withLock { withContext(Dispatchers.IO) {
        val parsed = Backup.parse(text)
        database.withTransaction {
            dao.clearLinks(); dao.clearItems(); dao.clearSeen(); dao.clearTopics(); dao.clearSources(); dao.clearSettings()
            val sourceIds = parsed.sources.associate { it.id to dao.addSource(it.copy(id = 0)) }
            val topicIds = parsed.topics.associate { it.id to dao.addTopic(it.copy(id = 0)) }
            parsed.links.forEach { dao.link(SourceTopic(sourceIds.getValue(it.sourceId), topicIds.getValue(it.topicId))) }
            parsed.saved.forEach { dao.addItem(it.copy(id = 0, sourceId = it.sourceId?.let(sourceIds::get))) }
            parsed.settings.forEach { dao.putSetting(it) }
        }
    } }
    companion object { val BACKUP_SETTINGS = setOf("theme", "wifiOnly", "images") }
}

data class Backup(val sources: List<Source>, val topics: List<Topic>, val links: List<SourceTopic>,
    val saved: List<Item>, val settings: List<Setting>) {
    companion object {
        fun parse(text: String): Backup {
            require(text.toByteArray().size <= 5_000_000) { "Backup exceeds the 5 MB limit." }
            val json = JSONObject(text)
            require(json.getString("format") == "localbrief" && json.getInt("version") == 1) { "Unsupported backup format." }
            fun rows(key: String): List<JSONObject> {
                val array = json.getJSONArray(key)
                require(array.length() <= 20_000) { "Backup contains too many records." }
                return (0 until array.length()).map { array.getJSONObject(it) }
            }
            fun JSONObject.bounded(key: String, max: Int): String = getString(key).also { require(it.length <= max) { "Invalid $key." } }
            val sources = rows("sources").map { Source(id = it.getLong("id"), name = it.bounded("name", 200),
                feedUrl = Rules.normalizeUrl(it.bounded("feedUrl", 4000)), kind = it.getString("kind").also { kind ->
                    require(kind in setOf("Website feed", "RSS feed", "YouTube channel")) { "Invalid source type." }
                }) }
            val topics = rows("topics").map { Topic(it.getLong("id"), it.bounded("name", 100), it.bounded("include", 500), it.bounded("exclude", 500)) }
            require(sources.all { it.id > 0 && it.name.isNotBlank() } && sources.map { it.id }.distinct().size == sources.size)
            require(sources.map { it.feedUrl }.distinct().size == sources.size)
            require(topics.all { it.id > 0 && it.name.isNotBlank() } && topics.map { it.id }.distinct().size == topics.size)
            require(topics.map { it.name }.distinct().size == topics.size)
            val sourceIds = sources.map { it.id }.toSet(); val topicIds = topics.map { it.id }.toSet()
            val links = rows("links").map { SourceTopic(it.getLong("sourceId"), it.getLong("topicId")) }
            require(links.all { it.sourceId in sourceIds && it.topicId in topicIds }) { "Invalid topic/source connection." }
            val saved = rows("saved").map {
                Item(sourceId = if (it.isNull("sourceId")) null else it.getLong("sourceId"), sourceName = it.bounded("sourceName", 200),
                    url = Rules.normalizeUrl(it.bounded("url", 4000)), title = it.bounded("title", 500), preview = it.bounded("preview", 1200),
                    publishedAt = if (it.isNull("publishedAt")) null else it.getLong("publishedAt"), firstSeen = it.getLong("firstSeen"),
                    thumbnail = if (it.isNull("thumbnail")) null else Rules.normalizeUrl(it.bounded("thumbnail", 4000)),
                    video = it.getBoolean("video"), savedAt = it.getLong("savedAt"))
            }
            require(saved.all { (it.sourceId == null || it.sourceId in sourceIds) && it.savedAt!! > 0 && it.firstSeen > 0 }) { "Invalid saved item." }
            require(saved.map { it.url }.distinct().size == saved.size)
            val settings = json.getJSONObject("settings")
            val values = Repository.BACKUP_SETTINGS.filter { settings.has(it) }.map { Setting(it, settings.getString(it)) }
            require(values.all { if (it.key == "theme") it.value in setOf("System", "Light", "Dark") else it.value in setOf("true", "false") }) { "Invalid settings." }
            return Backup(sources, topics, links, saved, values)
        }
    }
}
