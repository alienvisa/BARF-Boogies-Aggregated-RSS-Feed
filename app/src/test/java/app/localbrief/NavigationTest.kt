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
}
