package app.localbrief

import android.app.Application
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.localbrief.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

data class BriefState(val sources: List<Source> = emptyList(), val topics: List<Topic> = emptyList(),
    val links: List<SourceTopic> = emptyList(), val items: List<Item> = emptyList(), val settings: Map<String, String> = emptyMap())
data class Notice(val text: String, val undoItem: Item? = null)
class BriefViewModel(application: Application) : AndroidViewModel(application) {
    val repository = (application as BriefApplication).repository
    val state = combine(repository.dao.sources(), repository.dao.topics(), repository.dao.links(), repository.dao.items(), repository.dao.settings()) {
        sources, topics, links, items, settings -> BriefState(sources, topics, links, items, settings.associate { it.key to it.value })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, BriefState())
    val busy = MutableStateFlow(false)
    val ready = MutableStateFlow(false)
    var sessionBaseline = 0L
        private set
    private val notices = Channel<Notice>(Channel.BUFFERED)
    val messages = notices.receiveAsFlow()
    init {
        viewModelScope.launch {
            sessionBaseline = repository.dao.setting("lastVisit")?.toLongOrNull() ?: 0L
            repository.prune()
            ready.value = true
            refresh(quiet = true)
        }
    }
    fun act(success: String? = null, block: suspend () -> Unit) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            try { block(); success?.let { notices.send(Notice(it)) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { notices.send(Notice(if (e is android.database.sqlite.SQLiteConstraintException) "That source or topic already exists." else e.message ?: "Couldn't complete that action.")) }
            finally { busy.value = false }
        }
    }
    fun refresh(quiet: Boolean = false) = act {
        val connectivity = getApplication<Application>().getSystemService(ConnectivityManager::class.java)
        if (repository.dao.setting("wifiOnly") == "true" && connectivity.isActiveNetworkMetered) {
            if (!quiet) notices.send(Notice("Wi-Fi-only refresh is enabled. Connect to an unmetered network."))
        } else {
            val errors = repository.refresh()
            if (errors > 0) notices.send(Notice("$errors source(s) couldn't refresh. See Manage sources."))
            else if (!quiet) notices.send(Notice("Updates refreshed."))
        }
    }
    fun bookmark(item: Item) {
        viewModelScope.launch {
            repository.save(item, if (item.savedAt == null) System.currentTimeMillis() else null)
            if (item.savedAt != null) notices.send(Notice("Removed from Saved.", item))
        }
    }
    fun undo(item: Item) = act { repository.save(item, item.savedAt) }
    fun receive(text: String, title: String?) {
        // Shares are queued even if a refresh is in progress.
        viewModelScope.launch {
            try { repository.receiveLink(text, title); notices.send(Notice("Link saved on this device.")) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { notices.send(Notice(e.message ?: "Couldn't save that link.")) }
        }
    }
    fun visited(items: List<Item>) {
        if (!ready.value) return
        val latest = items.maxOfOrNull { it.firstSeen } ?: return
        viewModelScope.launch { repository.setting("lastVisit", latest.toString()) }
    }
    fun preference(key: String, value: String) = act {
        repository.setting(key, value)
        if (key == "wifiOnly") scheduleRefresh(getApplication(), value == "true")
    }
    fun restore(text: String) = act("Backup restored.") {
        repository.restoreBackup(text)
        sessionBaseline = 0
        scheduleRefresh(getApplication(), repository.dao.setting("wifiOnly") == "true")
    }
}
