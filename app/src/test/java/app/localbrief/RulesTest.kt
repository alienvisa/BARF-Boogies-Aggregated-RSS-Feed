package app.localbrief

import app.localbrief.data.Rules
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class RulesTest {
    @Test fun retentionIsExactly14Days() {
        assertEquals(TimeUnit.DAYS.toMillis(14), 2_000_000_000L - Rules.cutoff(2_000_000_000L))
    }
    @Test fun removesTrackingButPreservesFunctionalQuery() {
        assertEquals("https://example.com/story?id=2", Rules.normalizeUrl("https://EXAMPLE.com/story?id=2&utm_source=x#section"))
        assertEquals("https://www.youtube.com/watch?v=abc", Rules.normalizeUrl("https://www.youtube.com/watch?v=abc&utm_campaign=x"))
    }
    @Test fun excludesOverrideIncludes() {
        assertTrue(Rules.matches("Android update", "New features", "android, home lab", "rumor"))
        assertFalse(Rules.matches("Android rumor", "", "android", "rumor"))
        assertTrue(Rules.matches("Anything", "", "", ""))
    }
    @Test fun extractsLinkFromAndroidSharedText() {
        assertEquals("https://example.com/a", Rules.sharedLink("Read this: https://example.com/a."))
        assertNull(Rules.sharedLink("No link here"))
    }
    @Test fun youtubeShareAndFeedLinksResolveToSameVideo() {
        assertEquals("https://www.youtube.com/watch?v=abc123", Rules.normalizeUrl("https://youtu.be/abc123?si=tracking"))
        assertEquals("https://www.youtube.com/watch?v=abc123", Rules.normalizeUrl("https://m.youtube.com/watch?v=abc123&feature=share"))
        assertEquals("https://www.youtube.com/watch?v=abc123", Rules.normalizeUrl("https://www.youtube.com/shorts/abc123"))
    }
    @Test fun rejectsNonHttpsAndCredentialLinks() {
        listOf("javascript:alert(1)", "http://example.com", "https://user:secret@example.com/").forEach {
            assertTrue(runCatching { Rules.normalizeUrl(it) }.isFailure)
        }
    }
}
