package app.localbrief

import app.localbrief.ui.BrowserPolicy
import org.junit.Assert.*
import org.junit.Test

class BrowserPolicyTest {
    @Test fun secureArticlePathsAndAnchorsAreAllowed() {
        assertTrue(BrowserPolicy.canLoad("https://example.com/story?search=a%20b#section"))
    }
    @Test fun localAndExecutableSchemesAreBlocked() {
        listOf("file:///sdcard/backup.json", "content://app.localbrief/data", "javascript:alert(1)",
            "data:text/html,test", "intent://example", "http://example.com").forEach { assertFalse(BrowserPolicy.canLoad(it)) }
    }
    @Test fun credentialsAndMalformedLinksAreBlocked() {
        listOf("https://user:secret@example.com", "https://", "https:///story", "not a url").forEach { assertFalse(BrowserPolicy.canLoad(it)) }
    }
}
