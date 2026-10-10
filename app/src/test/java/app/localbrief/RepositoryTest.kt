package app.localbrief

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.localbrief.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class RepositoryTest {
    private lateinit var db: BriefDatabase
    private lateinit var repository: Repository
    private val provider = object : FeedProvider {
        override suspend fun discover(input: String) = Feed("Feed", input, listOf(FeedEntry("https://example.com/story", "Shared story", "Original preview", null, null, false)), "RSS feed")
        override suspend fun fetch(url: String) = discover(url)
    }
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BriefDatabase::class.java).allowMainThreadQueries().build()
        repository = Repository(db, provider)
    }
    @After fun close() { db.close() }
    @Test fun pruningPreservesSavedAndBoundaryItems() = runBlocking {
        val now = System.currentTimeMillis(); val cutoff = Rules.cutoff(now)
        db.dao().addItem(Item(sourceName = "Test", url = "https://example.com/old", title = "old", firstSeen = cutoff - 1))
        db.dao().addItem(Item(sourceName = "Test", url = "https://example.com/boundary", title = "boundary", firstSeen = cutoff))
        db.dao().addItem(Item(sourceName = "Test", url = "https://example.com/saved", title = "saved", firstSeen = 1, savedAt = 10))
        assertEquals(1, repository.prune(now))
        assertEquals(setOf("boundary", "saved"), db.dao().items().first().map { it.title }.toSet())
    }
    @Test fun deletingSourcePreservesBookmarksAndRemovesConnections() = runBlocking {
        val id = repository.addSource("https://example.com/feed", "Feed", emptySet())
        val item = db.dao().items().first().single()
        repository.save(item, 100)
        repository.removeSource(id)
        assertTrue(db.dao().allSources().isEmpty())
        assertNull(db.dao().allSaved().single().sourceId)
    }
    @Test fun duplicateAcrossSourcesRetainsBothMembershipsAndSharedSaveState() = runBlocking {
        repository.addSource("https://example.com/feed1", "One", emptySet())
        repository.addSource("https://example.com/feed2", "Two", emptySet())
        val items = db.dao().items().first()
        assertEquals(2, items.size)
        repository.save(items.first(), 100)
        assertTrue(db.dao().items().first().all { it.savedAt == 100L })
        repository.save(items.last(), null)
        assertTrue(db.dao().allSaved().isEmpty())
    }
    @Test fun expiredFeedEntryDoesNotReturnOnRefresh() = runBlocking {
        val id = repository.addSource("https://example.com/feed", "Feed", emptySet())
        val old = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(15)
        db.openHelper.writableDatabase.execSQL("UPDATE Item SET firstSeen = ?", arrayOf(old))
        db.openHelper.writableDatabase.execSQL("UPDATE SeenEntry SET firstSeen = ?", arrayOf(old))
        repository.prune()
        repository.refresh()
        assertTrue(db.dao().items().first().isEmpty())
        assertEquals(id, db.dao().seen(id).single().sourceId)
    }
    @Test fun sharedLinkCreatesBookmarkWithoutSubscribingAndDeduplicates() = runBlocking {
        repository.receiveLink("Look https://example.com/story", "Title")
        repository.receiveLink("https://example.com/story?utm_source=copy", "Title")
        assertEquals(1, db.dao().allSaved().size)
        assertTrue(db.dao().allSources().isEmpty())
    }
    @Test fun backupRoundTripAndMalformedImportAreAtomic() = runBlocking {
        val id = repository.addSource("https://example.com/feed", "Feed", emptySet())
        repository.topic("Technology", "chip, hardware", "rumor", setOf(id))
        repository.save(db.dao().items().first().single(), 100)
        repository.setting("theme", "Dark")
        val text = repository.exportBackup()
        repository.restoreBackup(text)
        assertEquals(1, db.dao().allSources().size)
        assertEquals(1, db.dao().allLinks().size)
        assertEquals(1, db.dao().allSaved().size)
        assertEquals("Dark", db.dao().setting("theme"))
        assertTrue(runCatching { repository.restoreBackup(text.replace("\"version\": 1", "\"version\": 99")) }.isFailure)
        assertEquals(1, db.dao().allSaved().size)
    }
}
