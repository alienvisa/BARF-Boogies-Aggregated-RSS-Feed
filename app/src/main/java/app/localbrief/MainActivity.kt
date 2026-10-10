package app.localbrief

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import app.localbrief.ui.BriefApp

data class IncomingShare(val text: String, val title: String?, val nonce: Long = System.nanoTime())
class MainActivity : ComponentActivity() {
    private var incoming by mutableStateOf<IncomingShare?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) receive(intent)
        val factory = ViewModelProvider.AndroidViewModelFactory(application)
        setContent { val model: BriefViewModel = viewModel(factory = factory); BriefApp(model, incoming) { incoming = null } }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }
    private fun receive(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
            incoming = IncomingShare(text, intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: intent.getStringExtra(Intent.EXTRA_TITLE))
        }
    }
}
