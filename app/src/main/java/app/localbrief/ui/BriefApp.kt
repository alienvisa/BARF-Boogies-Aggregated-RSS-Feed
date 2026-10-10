@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package app.localbrief.ui

import android.content.Intent
import androidx.core.net.toUri
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.localbrief.*
import app.localbrief.data.*
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable fun BriefApp(model: BriefViewModel, incoming: IncomingShare?, consumed: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val ready by model.ready.collectAsStateWithLifecycle()
    var route by rememberSaveable { mutableStateOf("Today") }
    var previous by rememberSaveable { mutableStateOf("Today") }
    var selectedTopic by rememberSaveable { mutableLongStateOf(0) }
    var selectedFilter by rememberSaveable { mutableLongStateOf(0) }
    var earlier by rememberSaveable { mutableStateOf(false) }
    var savedQuery by rememberSaveable { mutableStateOf("") }
    var savedType by rememberSaveable { mutableStateOf("All") }
    var sourceQuery by rememberSaveable { mutableStateOf("") }
    var topicEditor by remember { mutableStateOf(false) }
    var editingTopic by remember { mutableStateOf<Topic?>(null) }
    var sourceEditor by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<Source?>(null) }
    var removeSource by remember { mutableStateOf<Source?>(null) }
    var removeTopic by remember { mutableStateOf<Topic?>(null) }
    var restoreText by remember { mutableStateOf<String?>(null) }
    var articleUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var articleTitle by rememberSaveable { mutableStateOf("") }
    val todayScroll = rememberLazyListState()
    val topicScroll = rememberLazyListState()
    val savedScroll = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun back() { route = if (route == "Settings") previous else "Topics" }
    fun notify(text: String) { scope.launch { snackbar.showSnackbar(text) } }
    fun open(item: Item) {
        articleUrl = item.url
        articleTitle = item.title
    }
    fun share(item: Item) {
        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, item.url); putExtra(Intent.EXTRA_SUBJECT, item.title) }
        runCatching { context.startActivity(Intent.createChooser(intent, "Share link")) }.onFailure { notify("Couldn't open the share menu.") }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) model.act("Backup exported.") {
            val text = model.repository.exportBackup()
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } ?: error("Couldn't write backup.") }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.act {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
                    while (true) { val count = input.read(buffer); if (count < 0) break
                        require(output.size() + count <= 5_000_000) { "Backup exceeds the 5 MB limit." }; output.write(buffer, 0, count) }
                    output.toString("UTF-8")
                } ?: error("Couldn't read backup.")
            }
            withContext(Dispatchers.Default) { Backup.parse(text) }
            restoreText = text
        }
    }
    LaunchedEffect(incoming?.nonce) { incoming?.let { model.receive(it.text, it.title); articleUrl = null; route = "Saved"; consumed() } }
    LaunchedEffect(model) { model.messages.collect { notice ->
        val result = snackbar.showSnackbar(notice.text, if (notice.undoItem != null) "Undo" else null)
        if (result == SnackbarResult.ActionPerformed) notice.undoItem?.let(model::undo)
    } }
    LaunchedEffect(route, ready, state.items) { if (route == "Today" && ready) model.visited(state.items) }
    BackHandler(articleUrl == null && route !in listOf("Today", "Topics", "Saved")) { back() }
    BriefTheme(state.settings["theme"] ?: "System") { dark ->
        SideEffect {
            (context as? Activity)?.let { activity ->
                WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        val pageText = if (dark) WarmWhite else Forest
        val subtle = if (dark) Mint else MutedForest
        val images = state.settings["images"] != "false"
        CompositionLocalProvider(LocalContentColor provides pageText) {
            if (articleUrl != null) {
                val canonical = runCatching { Rules.normalizeUrl(articleUrl!!) }.getOrNull()
                val stored = state.items.firstOrNull { it.url == canonical && it.savedAt != null }
                    ?: state.items.firstOrNull { it.url == canonical }
                ArticleBrowser(articleUrl!!, articleTitle, dark, stored?.savedAt != null, snackbar,
                    onPage = { url, title -> articleUrl = url; articleTitle = title },
                    onSave = { if (stored?.savedAt != null) model.bookmark(stored) else model.receive(articleUrl!!, articleTitle) },
                    onClose = { articleUrl = null }, notify = ::notify)
            } else {
            Scaffold(containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    val active = when (route) { "Sources", "Topic" -> "Topics"; "Settings" -> previous; else -> route }
                    Column {
                        HorizontalDivider(color = if (dark) Sage else PaleSage)
                        NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                            listOf("Today" to Icons.Outlined.Home, "Topics" to Icons.Outlined.Tag, "Saved" to Icons.Outlined.BookmarkBorder).forEach { (name, icon) ->
                                NavigationBarItem(selected = active == name, onClick = { route = name },
                                    icon = { Icon(if (name == "Saved" && active == name) Icons.Filled.Bookmark else icon, name) },
                                    label = { Text(name) }, colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = if (dark) Mint else Forest, selectedTextColor = if (dark) Mint else Forest,
                                        indicatorColor = if (dark) Forest else Mint, unselectedIconColor = pageText, unselectedTextColor = pageText))
                            }
                        }
                    }
                }) { inset ->
                val modifier = Modifier.padding(inset).fillMaxSize()
                when (route) {
                    "Today", "Topic", "Saved" -> {
                        val topic = state.topics.find { it.id == if (route == "Topic") selectedTopic else selectedFilter }
                        val title = when (route) { "Topic" -> topic?.name ?: "Topic"; else -> route }
                        val list = when (route) {
                            "Saved" -> state.items.filter { it.savedAt != null && (savedType == "All" || it.video == (savedType == "Videos")) &&
                                "${it.title} ${it.preview} ${it.sourceName}".contains(savedQuery, ignoreCase = true) }.sortedByDescending { it.savedAt }
                            else -> state.items.filter { item ->
                                (earlier || route == "Topic" || item.firstSeen > model.sessionBaseline) &&
                                    (topic == null || (state.links.any { it.topicId == topic.id && it.sourceId == item.sourceId } &&
                                        Rules.matches(item.title, item.preview, topic.includeWords, topic.excludeWords)))
                            }
                        }.distinctBy { it.url }
                        PullToRefreshBox(isRefreshing = busy, onRefresh = { model.refresh() }, modifier = modifier) {
                            LazyColumn(state = when (route) { "Saved" -> savedScroll; "Topic" -> topicScroll; else -> todayScroll },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item {
                                    if (route == "Topic") BackRow("Back to Topics", pageText) { back() }
                                    Header(title, when (route) { "Saved" -> "Articles and videos for later"; "Topic" -> "Updates from your chosen sources"; else -> "New since your last visit" }, subtle,
                                        onSettings = { previous = if (route == "Topic") "Topics" else route; route = "Settings" },
                                        onRefresh = if (route == "Saved") null else { { model.refresh() } })
                                }
                                if (route == "Saved") {
                                    item { SearchField("Search saved items", savedQuery) { savedQuery = it } }
                                    item { FilterRow(listOf("All", "Articles", "Videos"), savedType, dark) { savedType = it } }
                                    item { Text("Newest saved first", color = subtle, style = MaterialTheme.typography.bodySmall) }
                                } else if (route == "Today") {
                                    item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Filter("All", selectedFilter == 0L, dark) { selectedFilter = 0 }
                                        state.topics.forEach { t -> Filter(t.name, selectedFilter == t.id, dark) { selectedFilter = t.id } }
                                    } }
                                } else if (topic != null) {
                                    item { PrimaryButton("Edit topic", Icons.Outlined.Edit) { editingTopic = topic; topicEditor = true } }
                                }
                                if (!ready) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Mint) }
                                if (list.isEmpty() && ready) item {
                                    EmptyCard(if (route == "Saved") if (savedQuery.isNotBlank()) "No matching saved items" else "Nothing saved yet"
                                        else if (state.sources.isEmpty()) "Choose your first source" else if (route == "Topic") "No matching updates" else "You're caught up",
                                        if (route == "Saved") "Save an update or share a link into BARF."
                                        else if (state.sources.isEmpty()) "Add a website feed, RSS feed, or YouTube channel."
                                        else "Refresh or browse earlier updates.", dark)
                                }
                                items(list, key = { it.id }) { item -> NewsCard(item, dark, images, { open(item) }, { model.bookmark(item) }, { share(item) }) }
                                if (route == "Today") item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { earlier = !earlier }) { Text(if (earlier) "New updates" else "Earlier updates", color = if (dark) Mint else Forest) }
                                        if (state.sources.isEmpty()) TextButton(onClick = { route = "Sources" }) { Text("Add sources", color = if (dark) Mint else Forest) }
                                    }
                                }
                            }
                        }
                    }
                    "Topics" -> LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { Header("Topics", "Your interests, your sources", subtle, { previous = "Topics"; route = "Settings" }) }
                        item { PrimaryButton("Add topic", Icons.Outlined.Add) { editingTopic = null; topicEditor = true } }
                        item { Text("Your topics", color = subtle, style = MaterialTheme.typography.titleLarge) }
                        if (state.topics.isEmpty()) item { EmptyCard("No topics yet", "Group your sources and optionally filter by keywords.", dark) }
                        items(state.topics, key = { it.id }) { topic ->
                            val connected = state.sources.filter { source -> state.links.any { it.topicId == topic.id && it.sourceId == source.id } }
                            ListCard(topic.name, "${connected.size} sources", connected.take(2).joinToString { it.name }.ifBlank { "Choose sources in Edit topic" } +
                                if (connected.size > 2) " +${connected.size - 2}" else "", dark) { selectedTopic = topic.id; route = "Topic" }
                        }
                        item { ListCard("Manage sources", "Add websites, feeds, and YouTube channels", null, dark, Icons.Outlined.RssFeed) { route = "Sources" } }
                    }
                    "Sources" -> LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { BackRow("Back to Topics", pageText) { back() }; Header("Manage sources", "Choose where your updates come from", subtle) }
                        item { PrimaryButton("Add source", Icons.Outlined.Add) { editingSource = null; sourceEditor = true } }
                        item { SearchField("Search sources", sourceQuery) { sourceQuery = it } }
                        item { Text("Your sources", color = subtle, style = MaterialTheme.typography.titleLarge) }
                        val sources = state.sources.filter { "${it.name} ${it.kind}".contains(sourceQuery, true) }
                        if (sources.isEmpty()) item { EmptyCard("No sources found", "Add a source or try a different search.", dark) }
                        items(sources, key = { it.id }) { source ->
                            val topics = state.topics.filter { topic -> state.links.any { it.sourceId == source.id && it.topicId == topic.id } }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ListCard(source.name, source.kind, "Topics: " + topics.joinToString { it.name }.ifBlank { "None" }, dark,
                                    if (source.kind == "YouTube channel") Icons.Outlined.PlayCircleOutline else Icons.Outlined.RssFeed) { editingSource = source; sourceEditor = true }
                                source.error?.let { Text("Could not refresh: $it", color = subtle, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                    "Settings" -> Column(modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        BackRow("Back", pageText) { back() }; Header("Settings", "Everything stays on this device", subtle)
                        Text("Appearance", style = MaterialTheme.typography.titleLarge)
                        FilterRow(listOf("System", "Light", "Dark"), state.settings["theme"] ?: "System", dark) { model.preference("theme", it) }
                        Toggle("Wi-Fi-only refresh", "Background refresh uses unmetered connections.", state.settings["wifiOnly"] == "true") { model.preference("wifiOnly", it.toString()) }
                        Toggle("Show thumbnails", "Small images use a bounded cache.", images) { model.preference("images", it.toString()) }
                        Text("Hourly refresh, subject to Android battery restrictions. Unsaved items are kept for 14 days after arrival. Saved items remain until removed.", color = subtle)
                        PrimaryButton("Refresh now", Icons.Outlined.Refresh) { model.refresh() }
                        Text("Backup and recovery", style = MaterialTheme.typography.titleLarge)
                        Text("Export sources, topics, saved links, and settings to a file you choose. Ordinary feed history is not included. Keep the file somewhere safe before uninstalling.", color = subtle)
                        PrimaryButton("Export backup", Icons.Outlined.FileDownload) { export.launch("local-brief-backup.json") }
                        OutlinedButton(onClick = { import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) { Text("Import backup", color = pageText) }
                        Text("No account, server, analytics, AI, or cloud backup. Feeds and images load directly. The article viewer contacts the publisher and the services embedded in its page.", color = subtle, style = MaterialTheme.typography.bodySmall)
                        Text("BARF · v0.1.2", color = subtle, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        }
        if (topicEditor) TopicEditor(editingTopic, state, busy, { topicEditor = false },
            { name, include, exclude, sources -> model.act("Topic saved.") { model.repository.topic(name, include, exclude, sources, editingTopic?.id ?: 0); topicEditor = false } },
            { editingTopic?.let { removeTopic = it } })
        if (sourceEditor) SourceEditor(editingSource, state, busy, { sourceEditor = false },
            { url, name, topics -> model.act("Source saved.") {
                val existing = editingSource
                if (existing == null) model.repository.addSource(url, name, topics) else model.repository.editSource(existing, name, topics)
                sourceEditor = false
            } }, { editingSource?.let { removeSource = it } })
        removeSource?.let { source -> Confirm("Remove ${source.name}?", "Its unsaved updates will be removed. Saved links will stay.", "Remove", { removeSource = null }) {
            model.act("Source removed.") { model.repository.removeSource(source.id); removeSource = null; sourceEditor = false }
        } }
        removeTopic?.let { topic -> Confirm("Remove ${topic.name}?", "Your sources and saved links will stay.", "Remove", { removeTopic = null }) {
            model.act("Topic removed.") { model.repository.removeTopic(topic.id); removeTopic = null; topicEditor = false; route = "Topics" }
        } }
        restoreText?.let { text -> Confirm("Replace local data?", "This backup will replace your sources, topics, bookmarks, and settings. Export a backup first if you want to keep the current data.", "Restore", { restoreText = null }) { restoreText = null; model.restore(text) } }
    }
}

@Composable private fun Header(title: String, subtitle: String, secondary: Color, onSettings: (() -> Unit)? = null, onRefresh: (() -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            if (onRefresh != null) IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "Refresh updates") }
            if (onSettings != null) IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Settings") }
        }
        Text(subtitle, color = secondary, style = MaterialTheme.typography.bodyLarge)
    }
}
@Composable private fun BackRow(label: String, color: Color, click: () -> Unit) {
    TextButton(onClick = click) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = color); Spacer(Modifier.width(8.dp)); Text(label, color = color) }
}
@Composable private fun PrimaryButton(label: String, icon: ImageVector, click: () -> Unit) {
    Button(onClick = click, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(100.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Forest)) {
        Icon(icon, null); Spacer(Modifier.width(8.dp)); Text(label)
    }
}
@Composable private fun SearchField(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value, change, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(label) },
        leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = RoundedCornerShape(12.dp),
        colors = fieldColors())
}
@Composable private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedTextColor = Forest, unfocusedTextColor = Forest, focusedLabelColor = Forest, unfocusedLabelColor = MutedForest,
    focusedLeadingIconColor = Forest, unfocusedLeadingIconColor = Forest, focusedBorderColor = Forest, unfocusedBorderColor = Sage)
@Composable private fun FilterRow(labels: List<String>, selected: String, dark: Boolean, choose: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEach { Filter(it, selected == it, dark) { choose(it) } }
    }
}
@Composable private fun Filter(label: String, selected: Boolean, dark: Boolean, choose: () -> Unit) {
    FilterChip(selected, choose, label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(100.dp),
        colors = FilterChipDefaults.filterChipColors(containerColor = if (dark) Forest else Color.White,
            labelColor = if (dark) WarmWhite else Forest, selectedContainerColor = Mint, selectedLabelColor = Forest),
        border = BorderStroke(1.dp, if (selected) Mint else Sage))
}
@Composable private fun ListCard(title: String, subtitle: String, detail: String?, dark: Boolean, icon: ImageVector? = null, click: () -> Unit) {
    Card(onClick = click, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (dark) WarmWhite else Color.White),
        border = if (dark) null else BorderStroke(1.dp, PaleSage), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (icon != null) Icon(icon, null, tint = Forest, modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Forest, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, color = MutedForest, style = MaterialTheme.typography.bodyMedium)
                if (detail != null) Text(detail, color = MutedForest, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Forest)
        }
    }
}
@Composable private fun EmptyCard(title: String, detail: String, dark: Boolean) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (dark) WarmWhite else Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Forest, style = MaterialTheme.typography.titleMedium); Text(detail, color = MutedForest)
        }
    }
}
@Composable private fun NewsCard(item: Item, dark: Boolean, images: Boolean, open: () -> Unit, save: () -> Unit, share: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (dark) WarmWhite else Color.White),
        border = if (dark) null else BorderStroke(1.dp, PaleSage), modifier = Modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalContentColor provides Forest) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (item.video) "VIDEO" else "ARTICLE", color = WarmWhite, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.background(Burgundy, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                    Text(item.sourceName + if (item.savedAt == null) item.publishedAt?.let { " · ${DateFormat.getDateInstance(DateFormat.SHORT).format(Date(it))}" }.orEmpty() else "",
                        style = MaterialTheme.typography.bodySmall, color = MutedForest, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        TextButton(onClick = open, contentPadding = PaddingValues(0.dp)) {
                            Text(item.title, color = Forest, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, "Open original", tint = Burgundy, modifier = Modifier.padding(start = 8.dp).size(20.dp))
                        }
                        if (item.preview.isNotBlank()) Text(item.preview, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (images && item.thumbnail != null) AsyncImage(item.thumbnail, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.padding(start = 8.dp).size(88.dp, 80.dp).clip(RoundedCornerShape(8.dp)))
                }
                HorizontalDivider(color = PaleSage)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = save, modifier = Modifier.semantics { stateDescription = if (item.savedAt == null) "Not saved" else "Saved" }) { Icon(if (item.savedAt == null) Icons.Outlined.BookmarkBorder else Icons.Filled.Bookmark,
                        if (item.savedAt == null) "Save item" else "Remove saved item", tint = Burgundy); Spacer(Modifier.width(8.dp)); Text(if (item.savedAt == null) "Save" else "Saved", color = Burgundy) }
                    TextButton(onClick = share) { Icon(Icons.Outlined.Share, null, tint = Burgundy); Spacer(Modifier.width(8.dp)); Text("Share", color = Burgundy) }
                }
            }
        }
    }
}
@Composable private fun Toggle(title: String, subtitle: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.toggleable(checked, role = Role.Switch, onValueChange = change).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall) }
        Switch(checked, null, colors = SwitchDefaults.colors(checkedTrackColor = Mint, checkedThumbColor = Forest))
    }
}
@Composable private fun Confirm(title: String, text: String, label: String, dismiss: () -> Unit, confirm: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = confirm) { Text(label, color = Burgundy) } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}
@Composable private fun TopicEditor(topic: Topic?, state: BriefState, busy: Boolean, dismiss: () -> Unit,
    submit: (String, String, String, Set<Long>) -> Unit, remove: () -> Unit) {
    var name by remember(topic?.id) { mutableStateOf(topic?.name.orEmpty()) }
    var include by remember(topic?.id) { mutableStateOf(topic?.includeWords.orEmpty()) }
    var exclude by remember(topic?.id) { mutableStateOf(topic?.excludeWords.orEmpty()) }
    var selected by remember(topic?.id) { mutableStateOf(state.links.filter { it.topicId == topic?.id }.map { it.sourceId }.toSet()) }
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, title = { Text(if (topic == null) "Add topic" else "Edit topic") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it.take(100) }, label = { Text("Topic name") }, singleLine = true)
            OutlinedTextField(include, { include = it.take(500) }, label = { Text("Include keywords (optional)") })
            OutlinedTextField(exclude, { exclude = it.take(500) }, label = { Text("Exclude keywords (optional)") })
            Text("Separate words or phrases with commas. Include matches any phrase; exclude hides matching items. Leave blank for all updates.", style = MaterialTheme.typography.bodySmall)
            Text("Sources", fontWeight = FontWeight.SemiBold)
            if (state.sources.isEmpty()) Text("Add sources through Manage sources, then connect them here.")
            state.sources.forEach { source -> CheckRow(source.name, source.id in selected) { selected = if (it) selected + source.id else selected - source.id } }
            if (topic != null) TextButton(onClick = remove, enabled = !busy) { Text("Remove topic", color = Burgundy) }
        }
    }, confirmButton = { TextButton(onClick = { submit(name, include, exclude, selected) }, enabled = !busy && name.isNotBlank()) { Text(if (busy) "Saving…" else "Save") } },
        dismissButton = { TextButton(onClick = dismiss, enabled = !busy) { Text("Cancel") } })
}
@Composable private fun SourceEditor(source: Source?, state: BriefState, busy: Boolean, dismiss: () -> Unit,
    submit: (String, String, Set<Long>) -> Unit, remove: () -> Unit) {
    var url by remember(source?.id) { mutableStateOf(source?.feedUrl.orEmpty()) }
    var name by remember(source?.id) { mutableStateOf(source?.name.orEmpty()) }
    var selected by remember(source?.id) { mutableStateOf(state.links.filter { it.sourceId == source?.id }.map { it.topicId }.toSet()) }
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, title = { Text(if (source == null) "Add source" else "Source settings") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (source == null) {
                OutlinedTextField(url, { url = it.take(4000) }, label = { Text("Website, feed, or YouTube channel") })
                Text("Paste an HTTPS feed/website URL, a YouTube channel URL, or a UC… channel ID. Some websites do not provide feeds.", style = MaterialTheme.typography.bodySmall)
            } else Text(source.feedUrl, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(name, { name = it.take(200) }, label = { Text(if (source == null) "Name (optional)" else "Name") }, singleLine = true)
            Text("Topics", fontWeight = FontWeight.SemiBold)
            if (state.topics.isEmpty()) Text("You can assign topics later. Updates will appear in Today.")
            state.topics.forEach { topic -> CheckRow(topic.name, topic.id in selected) { selected = if (it) selected + topic.id else selected - topic.id } }
            if (source != null) TextButton(onClick = remove, enabled = !busy) { Text("Remove source", color = Burgundy) }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = { submit(url, name, selected) }, enabled = !busy && (if (source == null) url.isNotBlank() else name.isNotBlank())) { Text(if (busy) "Checking…" else "Save") } },
        dismissButton = { TextButton(onClick = dismiss, enabled = !busy) { Text("Cancel") } })
}
@Composable private fun CheckRow(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.toggleable(checked, role = Role.Checkbox, onValueChange = change).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked, null); Text(label) }
}
