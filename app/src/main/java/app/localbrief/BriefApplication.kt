package app.localbrief

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.work.*
import app.localbrief.data.*
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache

open class BriefApplication : Application(), ImageLoaderFactory, Configuration.Provider {
    lateinit var repository: Repository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        repository = Repository(createDatabase())
        initializeBackgroundWork()
    }
    protected open fun createDatabase() = Room.databaseBuilder(this, BriefDatabase::class.java, "brief.db").build()
    protected open fun initializeBackgroundWork() {
        scope.launch { repository.prune(); scheduleRefresh(this@BriefApplication, repository.dao.setting("wifiOnly") == "true") }
    }
    override val workManagerConfiguration: Configuration get() = Configuration.Builder().build()
    override fun onTerminate() { scope.cancel(); repository.database.close(); super.onTerminate() }
    override fun newImageLoader() = ImageLoader.Builder(this)
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("thumbnails")).maxSizeBytes(20L * 1024 * 1024).build() }
        .build()
}
fun scheduleRefresh(context: Context, wifiOnly: Boolean) {
    val request = PeriodicWorkRequestBuilder<RefreshWorker>(1, TimeUnit.HOURS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true).build()).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork("feed-refresh", ExistingPeriodicWorkPolicy.UPDATE, request)
}
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = (applicationContext as BriefApplication).repository
        return try { if (repo.refresh() == 0) Result.success() else Result.retry() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { Result.retry() }
    }
}
