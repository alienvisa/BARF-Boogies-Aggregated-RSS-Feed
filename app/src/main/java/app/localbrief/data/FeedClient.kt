package app.localbrief.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.net.URI
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class FeedEntry(val url: String, val title: String, val preview: String,
    val publishedAt: Long?, val thumbnail: String?, val video: Boolean)
data class Feed(val name: String, val url: String, val entries: List<FeedEntry>, val kind: String)

interface FeedProvider {
    suspend fun discover(input: String): Feed
    suspend fun fetch(url: String): Feed
}
class FeedClient : FeedProvider {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false).build()
    override suspend fun discover(input: String): Feed = withContext(Dispatchers.IO) {
        val raw = input.trim()
        val channel = Regex("^UC[A-Za-z0-9_-]{22}$").matches(raw)
        var url = if (channel) "https://www.youtube.com/feeds/videos.xml?channel_id=$raw" else Rules.normalizeUrl(raw)
        val channelPath = Regex("youtube\\.com/channel/(UC[A-Za-z0-9_-]{22})").find(url)
        if (channelPath != null) url = "https://www.youtube.com/feeds/videos.xml?channel_id=${channelPath.groupValues[1]}"
        val (body, finalUrl) = download(url)
        runCatching { parse(body, finalUrl) }.getOrNull()?.let { return@withContext it }
        val html = Jsoup.parse(body, finalUrl)
        val candidates = html.select("link[rel=alternate][href]").filter {
            it.attr("type").lowercase() in listOf("application/rss+xml", "application/atom+xml")
        }
        val found = candidates.firstOrNull()?.absUrl("href")
        val channelId = Regex("\"channelId\":\"(UC[A-Za-z0-9_-]{22})\"").find(body)?.groupValues?.get(1)
        val feedUrl = found ?: channelId?.let { "https://www.youtube.com/feeds/videos.xml?channel_id=$it" }
            ?: error("No feed found. Paste an RSS/Atom feed URL or YouTube channel ID.")
        val (xml, resolved) = download(feedUrl)
        parse(xml, resolved)
    }
    override suspend fun fetch(url: String): Feed = withContext(Dispatchers.IO) {
        val (body, resolved) = download(url)
        parse(body, resolved)
    }
    private fun download(url: String): Pair<String, String> {
        var current = Rules.normalizeUrl(url)
        repeat(6) {
            client.newCall(Request.Builder().url(current).header("User-Agent", "LocalBrief/0.1 (Android RSS reader)").build()).execute().use { response ->
                if (response.code in 300..399) {
                    val next = response.header("Location") ?: error("Redirect without a destination.")
                    current = Rules.normalizeUrl(URI(current).resolve(next).toString())
                } else {
                    check(response.isSuccessful) { "Source returned HTTP ${response.code}." }
                    val body = response.body ?: error("Empty response.")
                    require(body.contentLength() <= 2_000_000) { "Source response is too large." }
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    val input = body.byteStream()
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break
                        require(output.size() + count <= 2_000_000) { "Source response is too large." }
                        output.write(buffer, 0, count)
                    }
                    val bytes = output.toByteArray()
                    require(bytes.size <= 2_000_000) { "Source response is too large." }
                    val charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                    return bytes.toString(charset) to current
                }
            }
        }
        error("Too many redirects.")
    }
    companion object {
        fun parse(xml: String, url: String): Feed {
            val document = Jsoup.parse(xml, url, Parser.xmlParser())
            val root = document.children().firstOrNull { it.tagName().lowercase() in setOf("rss", "feed", "rdf:rdf") }
                ?: error("This link is not an RSS or Atom feed.")
            val nodes = document.getElementsByTag("item").ifEmpty { document.getElementsByTag("entry") }
            fun Element.direct(tag: String) = children().firstOrNull { it.tagName().equals(tag, true) }
            val parent = root.direct("channel") ?: root
            val name = parent.direct("title")?.text()?.take(200)?.ifBlank { null } ?: URI(url).host
            val youtube = URI(url).host?.endsWith("youtube.com") == true
            val entries = nodes.take(250).mapNotNull { entry ->
                val linkNode = entry.children().firstOrNull {
                    it.tagName().equals("link", true) && (it.attr("rel").isBlank() || it.attr("rel") == "alternate")
                }
                val rawLink = linkNode?.attr("href")?.ifBlank { linkNode.text() }
                    ?: entry.direct("guid")?.takeIf { it.attr("isPermaLink") != "false" }?.text()
                val link = rawLink?.let { runCatching { Rules.normalizeUrl(URI(url).resolve(it).toString()) }.getOrNull() }
                    ?: return@mapNotNull null
                val title = entry.direct("title")?.text()?.take(500)?.ifBlank { null } ?: return@mapNotNull null
                val description = entry.direct("description") ?: entry.direct("summary") ?: entry.direct("content")
                    ?: entry.getElementsByTag("media:description").firstOrNull()
                val preview = description?.let { Jsoup.parse(it.text()).text().take(1200) }.orEmpty()
                val date = listOf("pubDate", "published", "updated", "dc:date").firstNotNullOfOrNull { entry.direct(it)?.text() }
                val time = date?.let { value ->
                    runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
                        ?: runCatching { ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }.getOrNull()
                }
                val thumb = entry.getElementsByTag("media:thumbnail").firstOrNull()?.attr("url")
                    ?.let { runCatching { Rules.normalizeUrl(it) }.getOrNull() }
                FeedEntry(link, title, preview, time, thumb, youtube || "youtube.com/watch" in link || "youtu.be/" in link)
            }
            return Feed(name, url, entries, if (youtube) "YouTube channel" else "RSS feed")
        }
    }
}
