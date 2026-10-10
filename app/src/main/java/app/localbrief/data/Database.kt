package app.localbrief.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(indices = [Index(value = ["feedUrl"], unique = true)])
data class Source(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String,
    val feedUrl: String, val kind: String, val lastChecked: Long = 0, val error: String? = null)
@Entity(indices = [Index(value = ["name"], unique = true)])
data class Topic(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String,
    val includeWords: String = "", val excludeWords: String = "")
@Entity(primaryKeys = ["sourceId", "topicId"])
data class SourceTopic(val sourceId: Long, val topicId: Long)
@Entity(indices = [Index(value = ["sourceId", "url"], unique = true), Index("url"), Index("firstSeen")])
data class Item(@PrimaryKey(autoGenerate = true) val id: Long = 0, val sourceId: Long? = null,
    val sourceName: String, val url: String, val title: String, val preview: String = "",
    val publishedAt: Long? = null, val firstSeen: Long, val thumbnail: String? = null,
    val video: Boolean = false, val savedAt: Long? = null)
@Entity data class Setting(@PrimaryKey val key: String, val value: String)
@Entity(primaryKeys = ["sourceId", "url"])
data class SeenEntry(val sourceId: Long, val url: String, val firstSeen: Long)

@Dao interface BriefDao {
    @Query("SELECT * FROM Source ORDER BY name COLLATE NOCASE") fun sources(): Flow<List<Source>>
    @Query("SELECT * FROM Topic ORDER BY name COLLATE NOCASE") fun topics(): Flow<List<Topic>>
    @Query("SELECT * FROM SourceTopic") fun links(): Flow<List<SourceTopic>>
    @Query("SELECT * FROM Item ORDER BY firstSeen DESC, id DESC") fun items(): Flow<List<Item>>
    @Query("SELECT * FROM Setting") fun settings(): Flow<List<Setting>>
    @Query("SELECT * FROM Source") suspend fun allSources(): List<Source>
    @Query("SELECT * FROM Topic") suspend fun allTopics(): List<Topic>
    @Query("SELECT * FROM SourceTopic") suspend fun allLinks(): List<SourceTopic>
    @Query("SELECT * FROM Item WHERE savedAt IS NOT NULL") suspend fun allSaved(): List<Item>
    @Query("SELECT * FROM Setting") suspend fun allSettings(): List<Setting>
    @Query("SELECT value FROM Setting WHERE `key` = :key") suspend fun setting(key: String): String?
    @Query("SELECT * FROM Item WHERE url = :url LIMIT 1") suspend fun itemByUrl(url: String): Item?
    @Insert suspend fun addSource(source: Source): Long
    @Update suspend fun updateSource(source: Source)
    @Insert suspend fun addTopic(topic: Topic): Long
    @Update suspend fun updateTopic(topic: Topic)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun link(link: SourceTopic)
    @Query("DELETE FROM SourceTopic WHERE sourceId = :id") suspend fun unlinkSource(id: Long)
    @Query("DELETE FROM SourceTopic WHERE topicId = :id") suspend fun unlinkTopic(id: Long)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addItem(item: Item): Long
    @Query("UPDATE Item SET savedAt = :time WHERE url = (SELECT url FROM Item WHERE id = :id)") suspend fun save(id: Long, time: Long?)
    @Query("SELECT * FROM SeenEntry WHERE sourceId = :id") suspend fun seen(id: Long): List<SeenEntry>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun remember(entry: SeenEntry)
    @Query("DELETE FROM SeenEntry WHERE sourceId = :id AND url NOT IN (:urls)") suspend fun trimSeen(id: Long, urls: List<String>)
    @Query("DELETE FROM SeenEntry WHERE sourceId = :id") suspend fun deleteSeen(id: Long)
    @Query("DELETE FROM SeenEntry") suspend fun clearSeen()
    @Query("UPDATE Item SET sourceName = :name WHERE sourceId = :id") suspend fun renameSourceItems(id: Long, name: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSetting(setting: Setting)
    @Query("DELETE FROM Item WHERE savedAt IS NULL AND firstSeen < :cutoff") suspend fun prune(cutoff: Long): Int
    @Query("DELETE FROM Item WHERE sourceId = :id AND savedAt IS NULL") suspend fun deleteSourceItems(id: Long)
    @Query("UPDATE Item SET sourceId = NULL WHERE sourceId = :id") suspend fun detachSaved(id: Long)
    @Query("DELETE FROM Source WHERE id = :id") suspend fun deleteSource(id: Long)
    @Query("DELETE FROM Topic WHERE id = :id") suspend fun deleteTopic(id: Long)
    @Query("DELETE FROM Item") suspend fun clearItems()
    @Query("DELETE FROM SourceTopic") suspend fun clearLinks()
    @Query("DELETE FROM Source") suspend fun clearSources()
    @Query("DELETE FROM Topic") suspend fun clearTopics()
    @Query("DELETE FROM Setting") suspend fun clearSettings()
}
@Database(entities = [Source::class, Topic::class, SourceTopic::class, Item::class, Setting::class, SeenEntry::class], version = 1, exportSchema = true)
abstract class BriefDatabase : RoomDatabase() { abstract fun dao(): BriefDao }
