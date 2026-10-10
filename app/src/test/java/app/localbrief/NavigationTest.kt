package app.localbrief

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import androidx.room.Room
import app.localbrief.data.BriefDatabase
import java.util.concurrent.Executor

class UiTestApplication : BriefApplication() {
    override fun createDatabase() = Room.inMemoryDatabaseBuilder(this, BriefDatabase::class.java)
        .allowMainThreadQueries().setQueryExecutor(Executor { it.run() }).setTransactionExecutor(Executor { it.run() }).build()
    override fun initializeBackgroundWork() = Unit
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = UiTestApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun emptyLibraryCanReachSourcesAndSavedWithoutLogin() {
        compose.onAllNodesWithText("Topics").onLast().performClick()
        compose.onNodeWithText("Manage sources").performClick()
        compose.onNodeWithText("Choose where your updates come from").assertIsDisplayed()
        compose.onNodeWithText("Add source").performClick()
        compose.onNodeWithText("Website, feed, or YouTube channel").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onAllNodesWithText("Saved").onLast().performClick()
        compose.onNodeWithText("Search saved items").assertIsDisplayed()
        compose.onNodeWithText("Articles and videos for later").assertIsDisplayed()
    }
    @Test fun settingsExposeLocalBackupAndFourteenDayPolicy() {
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Everything stays on this device").assertIsDisplayed()
        compose.onNodeWithText("Dark").performClick()
        compose.onNodeWithText("Hourly refresh, subject to Android battery restrictions. Unsaved items are kept for 14 days after arrival. Saved items remain until removed.").assertExists()
        compose.onNodeWithText("Export backup").assertExists()
    }
    @Test fun savedArticleOpensInsideAppAndClosesBackToSaved() {
        val application = compose.activity.application as UiTestApplication
        kotlinx.coroutines.runBlocking { application.repository.receiveLink("https://example.com/article", "Sample article") }
        compose.onAllNodesWithText("Saved").onLast().performClick()
        compose.waitUntil(timeoutMillis = 5000) { compose.onAllNodesWithText("Sample article").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Sample article").performClick()
        compose.onNodeWithContentDescription("Close article").assertIsDisplayed()
        compose.onNodeWithText("example.com").assertIsDisplayed()
        compose.onNodeWithContentDescription("Open externally").assertIsDisplayed()
        compose.runOnIdle {
            val viewer = findViewer(compose.activity.window.decorView)!!
            org.junit.Assert.assertFalse(viewer.settings.allowFileAccess)
            org.junit.Assert.assertFalse(viewer.settings.allowContentAccess)
            org.junit.Assert.assertEquals(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW, viewer.settings.mixedContentMode)
            org.junit.Assert.assertTrue(viewer.settings.mediaPlaybackRequiresUserGesture)
        }
        compose.onNodeWithContentDescription("Close article").performClick()
        compose.onNodeWithText("Search saved items").assertIsDisplayed()
    }
    @Test fun browserSavesFollowedPageWithoutReplacingOriginalBookmark() {
        val application = compose.activity.application as UiTestApplication
        kotlinx.coroutines.runBlocking { application.repository.receiveLink("https://example.com/original-follow-test", "Original article") }
        compose.onAllNodesWithText("Saved").onLast().performClick()
        try {
            compose.waitUntil(timeoutMillis = 5000) { compose.onAllNodesWithText("Original article").fetchSemanticsNodes().isNotEmpty() }
        } catch (failure: Throwable) {
            val saved = kotlinx.coroutines.runBlocking { application.repository.dao.allSaved() }
            val vm = androidx.lifecycle.ViewModelProvider(compose.activity)[BriefViewModel::class.java]
            throw AssertionError("Ready: ${vm.ready.value}; state: ${vm.state.value}; lifecycle: ${compose.activity.lifecycle.currentState}; Saved fixture: $saved; UI: " + compose.onRoot(useUnmergedTree = true).printToString(), failure)
        }
        compose.onNodeWithText("Original article").performClick()
        compose.runOnIdle {
            val viewer = findViewer(compose.activity.window.decorView)!!
            viewer.webViewClient.onPageStarted(viewer, "https://example.org/followed?utm_source=feed#section", null)
            viewer.webChromeClient!!.onReceivedTitle(viewer, "Followed article")
            viewer.webViewClient.onPageFinished(viewer, "https://example.org/followed?utm_source=feed#section")
        }
        compose.onNodeWithText("example.org").assertIsDisplayed()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(timeoutMillis = 5000) {
            kotlinx.coroutines.runBlocking { application.repository.dao.itemByUrl("https://example.org/followed")?.savedAt != null }
        }
        org.junit.Assert.assertNotNull(kotlinx.coroutines.runBlocking {
            application.repository.dao.itemByUrl("https://example.com/original-follow-test")?.savedAt
        })
        compose.waitUntil(timeoutMillis = 5000) { compose.onAllNodesWithText("Saved").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Close article").performClick()
        compose.onNodeWithText("Followed article").assertExists()
    }

    private fun findViewer(view: android.view.View): android.webkit.WebView? {
        if (view is android.webkit.WebView) return view
        if (view is android.view.ViewGroup) for (index in 0 until view.childCount) {
            findViewer(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}
