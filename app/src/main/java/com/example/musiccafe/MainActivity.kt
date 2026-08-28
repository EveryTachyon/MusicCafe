package com.example.musiccafe

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musiccafe.ui.theme.MusicCafeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SidebarBackground = Color(0xFF100E13)
private val ContentBackground = Color(0xFF18161C)
private val SidebarText = Color(0xFF8B898C)
private val AccentGreen = Color(0xFF00D51B)
private val CardBackground = Color(0xFF242128)
private val SoftText = Color(0xFFB9B6BC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicCafeTheme {
                MusicCafeApp()
            }
        }
    }
}

@Composable
private fun MusicCafeApp() {
    val context = LocalContext.current
    var selectedItem by remember { mutableStateOf("Home") }
    var importedSongs by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var playingSong by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var downloadedSongs by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var googleDriveEmail by remember { mutableStateOf<String?>(null) }
    var pendingGoogleDriveEmail by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val chooseFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { folderUri ->
        if (folderUri != null) {
            importedSongs = scanAudioFiles(context.contentResolver, folderUri)
        }
    }
    val chooseAudioFiles = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { audioUris ->
        importedSongs = audioUris
    }
    val chooseGoogleDriveFiles = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { folderUri ->
        if (folderUri != null) {
            googleDriveEmail = pendingGoogleDriveEmail
            val audioUris = scanAudioFiles(context.contentResolver, folderUri)
            importedSongs = audioUris
            scope.launch {
                val downloaded = withContext(Dispatchers.IO) {
                    audioUris.filter { uri -> downloadAudioFile(context, uri) }.toSet()
                }
                downloadedSongs = downloaded
            }
        }
    }
    val chooseGoogleAccount = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val accountEmail = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (result.resultCode == Activity.RESULT_OK && !accountEmail.isNullOrBlank()) {
            pendingGoogleDriveEmail = accountEmail
            chooseGoogleDriveFiles.launch(null)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(ContentBackground)) {
        Box(modifier = Modifier.weight(1f)) {
            LandingContent(
                selectedItem = selectedItem,
                onOpenSidebar = {},
                importedSongs = importedSongs,
                downloadedSongs = downloadedSongs,
                onChooseFolder = { chooseFolder.launch(null) },
                onChooseAudioFiles = { chooseAudioFiles.launch(arrayOf("audio/*")) },
                onOpenSavedSongs = { selectedItem = "Saved songs" },
                onBackToLibrary = { selectedItem = "Library" },
                onBackFromImport = { selectedItem = "Library" },
                onPlaySong = { song -> playingSong = song; isPlaying = true },
                onOpenImportSongs = { selectedItem = "Import songs" },
                onOpenCreatePlaylist = { selectedItem = "Create playlist" },
                playlists = playlists,
                onSavePlaylist = { name, songs ->
                    playlists = playlists + Playlist(name, songs)
                    selectedItem = "Library"
                },
                googleDriveEmail = googleDriveEmail,
                onChooseGoogleDriveFiles = {
                    chooseGoogleAccount.launch(
                        AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null)
                    )
                }
            )
        }
        if (playingSong != null) {
            MiniPlayer(playingSong!!, isPlaying) { isPlaying = !isPlaying }
        }
        BottomNavigationBar(selectedItem) { selectedItem = it }
    }
}

@Composable
private fun BottomNavigationBar(selectedItem: String, onItemSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF100E13)).padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        BottomNavigationItem("Home", Icons.Outlined.Home, selectedItem, onItemSelected)
        BottomNavigationItem("Library", Icons.Outlined.LibraryMusic, selectedItem, onItemSelected)
    }
}

@Composable
private fun BottomNavigationItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.width(76.dp).clickable { onItemSelected(label) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = if (selectedItem == label) AccentGreen else SidebarText, modifier = Modifier.height(30.dp))
        Text(label, color = if (selectedItem == label) AccentGreen else SidebarText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LandingContent(
    selectedItem: String,
    onOpenSidebar: () -> Unit,
    importedSongs: List<Uri>,
    downloadedSongs: Set<Uri>,
    onChooseFolder: () -> Unit,
    onChooseAudioFiles: () -> Unit,
    onOpenSavedSongs: () -> Unit,
    onBackToLibrary: () -> Unit,
    onBackFromImport: () -> Unit,
    onPlaySong: (String) -> Unit,
    onOpenImportSongs: () -> Unit,
    onOpenCreatePlaylist: () -> Unit,
    playlists: List<Playlist>,
    onSavePlaylist: (String, Set<Uri>) -> Unit,
    googleDriveEmail: String?,
    onChooseGoogleDriveFiles: () -> Unit
) {
    if (selectedItem == "Import songs") {
        ImportSongsContent(
            onBack = onBackFromImport,
            importedSongs = importedSongs,
            downloadedSongs = downloadedSongs,
            googleDriveEmail = googleDriveEmail,
            onChooseAudioFiles = onChooseAudioFiles,
            onChooseGoogleDriveFiles = onChooseGoogleDriveFiles
        )
        return
    }

    when (selectedItem) {
        "Library" -> LibraryContent(importedSongs, onOpenSidebar, onChooseAudioFiles, onOpenSavedSongs, playlists, onOpenCreatePlaylist)
        "Saved songs" -> SavedSongsContent(importedSongs, onChooseAudioFiles, onBackToLibrary, onPlaySong, onOpenImportSongs)
        "Create playlist" -> CreatePlaylistContent(importedSongs, downloadedSongs, onSavePlaylist, onBackToLibrary)
        else -> HomeContent(onOpenSidebar, onOpenSavedSongs, importedSongs, playlists)
    }
}

@Composable
private fun HomeContent(
    onOpenSidebar: () -> Unit,
    onOpenSavedSongs: () -> Unit,
    importedSongs: List<Uri>,
    playlists: List<Playlist>
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("MusicCafe", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSidebar) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }
        if (importedSongs.isNotEmpty() || playlists.isNotEmpty()) {
            item {
                Text("Saved songs", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 22.dp))
            }
            item {
                LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (importedSongs.isNotEmpty()) {
                        item { HomeCollectionCard("Saved songs", onOpenSavedSongs) }
                    }
                    items(playlists) { playlist -> HomeCollectionCard(playlist.name) }
                }
            }
        }
    }
}

@Composable
private fun HomeCollectionCard(title: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.width(250.dp).height(86.dp).background(CardBackground, RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(96.dp).fillMaxHeight().background(Color(0xFF1F1D23), RoundedCornerShape(12.dp)))
        Text(title, color = Color.White, fontSize = 17.sp, maxLines = 2, modifier = Modifier.padding(horizontal = 14.dp))
    }
}

@Composable
private fun PlaylistCard(title: String, color: Color) {
    Column(modifier = Modifier.width(160.dp).height(166.dp).background(color, RoundedCornerShape(12.dp)).padding(14.dp)) {
        Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.height(28.dp))
        Spacer(Modifier.weight(1f))
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AdvertisingBlock() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("ADVERTISING", color = Color.White, fontSize = 14.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 42.dp).height(78.dp).background(CardBackground, RoundedCornerShape(12.dp)))
    }
}

@Composable
private fun LibraryContent(
    importedSongs: List<Uri>,
    onOpenSidebar: () -> Unit,
    onChooseAudioFiles: () -> Unit,
    onOpenSavedSongs: () -> Unit,
    playlists: List<Playlist>,
    onOpenCreatePlaylist: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Library", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSidebar) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }
        item { LibrarySummaryRow("Saved songs", "${importedSongs.size} songs", Icons.Outlined.LibraryMusic, onOpenSavedSongs) }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Playlists", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.List, contentDescription = "Playlists", tint = Color.White, modifier = Modifier.height(28.dp))
            }
        }
        item {
            Row(modifier = Modifier.padding(horizontal = 22.dp).clickable(onClick = onOpenCreatePlaylist), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(86.dp).background(CardBackground, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Text("+", color = Color.White, fontSize = 42.sp)
                }
                Text("Create playlist", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(start = 18.dp))
            }
        }
        items(playlists) { playlist ->
            Text(playlist.name, color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 22.dp))
        }
    }
}

@Composable
private fun LibrarySummaryRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).clickable(enabled = onClick != null) { onClick?.invoke() }, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(86.dp).background(CardBackground, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = SoftText, modifier = Modifier.height(34.dp))
        }
        Column(modifier = Modifier.padding(start = 18.dp)) {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = SoftText, fontSize = 18.sp)
        }
    }
}

@Composable
private fun CreatePlaylistContent(
    importedSongs: List<Uri>,
    downloadedSongs: Set<Uri>,
    onSavePlaylist: (String, Set<Uri>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var playlistName by remember { mutableStateOf("") }
    var selectedSongs by remember(downloadedSongs, importedSongs) {
        mutableStateOf(if (downloadedSongs.isNotEmpty()) downloadedSongs else importedSongs.toSet())
    }
    val songNames = remember(importedSongs) {
        importedSongs.map { uri -> uri to displayName(context.contentResolver, uri) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Create playlist", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
        BasicTextField(
            value = playlistName,
            onValueChange = { playlistName = it },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 20.sp),
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth().background(CardBackground, RoundedCornerShape(12.dp)).padding(18.dp)) {
                    if (playlistName.isEmpty()) Text("Playlist name", color = SoftText, fontSize = 20.sp)
                    innerTextField()
                }
            }
        )
        Text("Choose songs", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 26.dp, bottom = 8.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(songNames) { (uri, songName) ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(CardBackground, RoundedCornerShape(8.dp)).clickable {
                        selectedSongs = if (uri in selectedSongs) selectedSongs - uri else selectedSongs + uri
                    }.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = uri in selectedSongs,
                        onCheckedChange = { checked ->
                            selectedSongs = if (checked) selectedSongs + uri else selectedSongs - uri
                        }
                    )
                    Text(songName, color = Color.White, fontSize = 17.sp, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().background(AccentGreen, RoundedCornerShape(28.dp)).clickable(enabled = playlistName.isNotBlank()) {
                onSavePlaylist(playlistName.trim(), selectedSongs)
            }.padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("SAVE PLAYLIST", color = Color(0xFF071014), fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun ImportSongsContent(
    onBack: () -> Unit,
    importedSongs: List<Uri>,
    downloadedSongs: Set<Uri>,
    googleDriveEmail: String?,
    onChooseAudioFiles: () -> Unit,
    onChooseGoogleDriveFiles: () -> Unit
) {
    val context = LocalContext.current
    val songNames = remember(importedSongs) {
        importedSongs.map { uri -> uri to displayName(context.contentResolver, uri) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "Back to Library",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Import songs",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
        item {
            ImportSourceRow("Device", onChooseAudioFiles)
        }
        item {
            if (googleDriveEmail != null) {
                GoogleDriveAccountCard(googleDriveEmail, onChooseGoogleDriveFiles)
            } else {
                ImportSourceRow("Google Drive", onChooseGoogleDriveFiles)
            }
        }
        if (importedSongs.isNotEmpty()) {
            item {
                Text(
                    text = "Music",
                    color = AccentGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(songNames) { (uri, songName) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(8.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = songName, color = Color.White, fontSize = 19.sp)
                        Text(
                            text = if (uri in downloadedSongs) "Downloaded" else "Not downloaded",
                            color = if (uri in downloadedSongs) AccentGreen else SoftText,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    androidx.compose.material3.Checkbox(
                        checked = uri in downloadedSongs,
                        onCheckedChange = null
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportSourceRow(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
            .background(CardBackground, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = Color.White,
            fontSize = 27.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GoogleDriveAccountCard(email: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 36.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Google Drive",
                        color = Color.White,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "285 MB / 15 GB used",
                        color = SoftText,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                CircularProgressIndicator(
                    progress = { 0.019f },
                    color = AccentGreen,
                    trackColor = SidebarBackground,
                    strokeWidth = 8.dp,
                    modifier = Modifier.width(56.dp).height(56.dp)
                )
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .height(1.dp)
                    .background(Color(0xFF3A3740))
            )
            Text(
                text = email,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 18.dp)
            )
        }
    }
}

private fun displayName(contentResolver: android.content.ContentResolver, uri: Uri): String {
    val nameColumn = android.provider.OpenableColumns.DISPLAY_NAME
    contentResolver.query(uri, arrayOf(nameColumn), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndexOrThrow(nameColumn))
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "Audio file"
}

private fun downloadAudioFile(context: android.content.Context, uri: Uri): Boolean {
    val fileName = displayName(context.contentResolver, uri)
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
    val destination = java.io.File(context.filesDir, fileName)
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } != null
    } catch (_: java.io.IOException) {
        false
    }
}

private data class PageDetails(
    val title: String,
    val subtitle: String,
    val cards: List<String>
)

@Preview(showBackground = true, widthDp = 900, heightDp = 800)
@Composable
private fun MusicCafePreview() {
    MusicCafeTheme(dynamicColor = false, darkTheme = true) {
        MusicCafeApp()
    }
}

private fun scanAudioFiles(contentResolver: android.content.ContentResolver, treeUri: Uri): List<Uri> {
    val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
    val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocumentId)
    return scanAudioFiles(contentResolver, childUri, treeUri)
}

private fun scanAudioFiles(
    contentResolver: android.content.ContentResolver,
    directoryUri: Uri,
    treeUri: Uri
): List<Uri> {
    val audioFiles = mutableListOf<Uri>()
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_MIME_TYPE
    )

    contentResolver.query(directoryUri, projection, null, null, null)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
        while (cursor.moveToNext()) {
            val documentId = cursor.getString(idColumn)
            val mimeType = cursor.getString(mimeColumn)
            val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
            if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
                audioFiles += scanAudioFiles(contentResolver, childrenUri, treeUri)
            } else if (mimeType.startsWith("audio/")) {
                audioFiles += documentUri
            }
        }
    }
    return audioFiles
}

@Composable
private fun SavedSongsContent(
    importedSongs: List<Uri>,
    onChooseAudioFiles: () -> Unit,
    onBack: () -> Unit,
    onPlaySong: (String) -> Unit,
    onOpenImportSongs: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val songNames = remember(importedSongs) {
        importedSongs.map { uri -> uri to displayName(context.contentResolver, uri) }
    }
    val visibleSongs = songNames.filter { (_, name) ->
        searchQuery.isBlank() || name.contains(searchQuery, ignoreCase = true)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F0E13)).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.List, contentDescription = "Song list", tint = Color.White, modifier = Modifier.height(34.dp))
            }
        }
        item {
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 20.sp),
                modifier = Modifier.fillMaxWidth().height(58.dp),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxSize().background(Color(0xFF39373E), RoundedCornerShape(32.dp)).padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search songs", tint = SoftText, modifier = Modifier.height(26.dp))
                        Box(modifier = Modifier.padding(start = 18.dp)) {
                            if (searchQuery.isEmpty()) Text("Search", color = SoftText, fontSize = 20.sp)
                            innerTextField()
                        }
                    }
                }
            )
        }
        item {
            Text("Saved songs", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            Row(modifier = Modifier.border(2.dp, Color(0xFF47444C), RoundedCornerShape(32.dp)).padding(horizontal = 24.dp, vertical = 12.dp).clickable(onClick = onOpenImportSongs), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.height(26.dp))
                Text("IMPORT SONGS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
        items(visibleSongs) { (songUri, songName) ->
                Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp).clickable { onPlaySong(songName) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(AccentGreen, androidx.compose.foundation.shape.CircleShape))
                        Text(songName, color = Color.White, fontSize = 18.sp, maxLines = 1, modifier = Modifier.weight(1f).padding(start = 10.dp))
                        Text("••", color = SoftText, fontSize = 24.sp)
                    }
                    Text("Cosmograph", color = SoftText, fontSize = 17.sp, modifier = Modifier.padding(start = 20.dp, top = 3.dp))
                }
        }
    }
}

@Composable
private fun MiniPlayer(songTitle: String, isPlaying: Boolean, onTogglePlaying: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(78.dp).padding(horizontal = 6.dp).background(Color(0xFF4A494F), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(58.dp).background(Color(0xFF77767A), RoundedCornerShape(8.dp)))
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(songTitle, color = Color.White, fontSize = 18.sp, maxLines = 1)
            Text("Cosmograph", color = SoftText, fontSize = 16.sp, maxLines = 1)
        }
        Icon(Icons.Outlined.Cast, contentDescription = "Cast", tint = Color.White, modifier = Modifier.size(28.dp))
        IconButton(onClick = onTogglePlaying) {
            Icon(if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = if (isPlaying) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}

private data class Playlist(val name: String, val songs: Set<Uri>)