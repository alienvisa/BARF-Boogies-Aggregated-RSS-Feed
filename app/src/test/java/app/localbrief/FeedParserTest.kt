package app.localbrief

import app.localbrief.data.FeedClient
import org.junit.Assert.*
import org.junit.Test

class FeedParserTest {
    @Test fun readsRssOriginalPreviewWithoutMarkup() {
        val feed = FeedClient.parse("""<rss version="2.0"><channel><title>Example</title><item><title>News &amp; updates</title><link>https://example.com/a</link><description><![CDATA[<p>Hello <b>world</b></p>]]></description><pubDate>Fri, 10 Oct 2025 12:00:00 GMT</pubDate></item></channel></rss>""", "https://example.com/feed")
        assertEquals("Example", feed.name)
        assertEquals("News & updates", feed.entries.single().title)
        assertEquals("Hello world", feed.entries.single().preview)
        assertNotNull(feed.entries.single().publishedAt)
    }
    @Test fun resolvesAtomLinksAndPrefersAlternateOverSelf() {
        val feed = FeedClient.parse("""<feed xmlns="http://www.w3.org/2005/Atom"><title>Atom</title><entry><title>Item</title><link rel="self" href="/entry.xml"/><link rel="alternate" href="/story"/><summary>Source excerpt</summary><published>2026-10-10T12:00:00Z</published></entry></feed>""", "https://example.com/feed")
        assertEquals("https://example.com/story", feed.entries.single().url)
        assertEquals("Source excerpt", feed.entries.single().preview)
    }
    @Test fun youtubeMediaDescriptionAndThumbnail() {
        val feed = FeedClient.parse("""<feed xmlns:media="http://search.yahoo.com/mrss/"><title>Channel</title><entry><title>Video</title><link rel="alternate" href="https://www.youtube.com/watch?v=123"/><media:group><media:description>A channel description</media:description><media:thumbnail url="https://i.ytimg.com/vi/123/default.jpg"/></media:group></entry></feed>""", "https://www.youtube.com/feeds/videos.xml?channel_id=UCexample")
        assertTrue(feed.entries.single().video)
        assertEquals("A channel description", feed.entries.single().preview)
        assertNotNull(feed.entries.single().thumbnail)
    }
    @Test fun missingPreviewStaysEmptyAndUnsafeLinkIsSkipped() {
        val feed = FeedClient.parse("""<rss><channel><title>Feed</title><item><title>Safe</title><link>https://example.com/a</link></item><item><title>Unsafe</title><link>javascript:alert(1)</link></item></channel></rss>""", "https://example.com/feed")
        assertEquals(1, feed.entries.size)
        assertEquals("", feed.entries.single().preview)
    }
    @Test fun rejectsHtmlPagesInsteadOfCallingThemEmptyFeeds() {
        assertTrue(runCatching { FeedClient.parse("<html><title>Website</title></html>", "https://example.com/") }.isFailure)
    }
}
