package com.example.musiccafe

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.DocumentsContract
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.net.toFile
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.List
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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

private data class Playlist(val name: String, val songs: Set<Uri>)

class MainActivity : ComponentActivity(), MediaPlaybackService.PlaybackListener {
    private var playbackService: MediaPlaybackService? = null
    private var isBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MediaPlaybackService.MediaPlaybackBinder
            playbackService = binder.getService()
            playbackService?.setPlaybackListener(this@MainActivity)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        bindToService()
        setContent {
            MusicCafeTheme {
                MusicCafeApp(this@MainActivity)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
    
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    
    private fun bindToService() {
        val intent = Intent(this, MediaPlaybackService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    fun getPlaybackService(): MediaPlaybackService? = playbackService
    
    // PlaybackListener implementation
    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        // Notify UI of playback state change if needed
    }
    
    override fun onSkipNext() {
        // Handle skip next action
    }
    
    override fun onSkipPrev() {
        // Handle skip previous action
    }
    
    override fun onSongCompleted() {
        // Handle song completion
    }
    
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 100
    }
}

@Composable
private fun MusicCafeApp(activity: MainActivity) {
    val context = LocalContext.current
    var selectedItem by remember { mutableStateOf("Home") }
    var importedSongs by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var playingSong by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var downloadedSongs by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    val chooseAudioFiles = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { audioUris ->
        val validAudioUris = audioUris.filter { uri -> isSupportedAudioUri(context.contentResolver, uri) }
        validAudioUris.forEach { uri -> persistReadPermission(context, uri) }
        importedSongs = (importedSongs + validAudioUris).distinct()
    }
    val chooseGoogleDriveFiles = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (!isSupportedAudioUri(context.contentResolver, uri)) return@rememberLauncherForActivityResult
        persistReadPermission(context, uri)
        val savedUri = copyUriToAppStorage(context, uri)
        if (savedUri != null) {
            importedSongs = (importedSongs + savedUri).distinct()
            downloadedSongs = downloadedSongs + savedUri
        }
    }
    Column(modifier = Modifier.fillMaxSize().background(ContentBackground)) {
        Box(modifier = Modifier.weight(1f)) {
            LandingContent(
                selectedItem = selectedItem,
                importedSongs = importedSongs,
                downloadedSongs = downloadedSongs,
                onChooseAudioFiles = { chooseAudioFiles.launch(arrayOf("audio/*")) },
                onOpenSavedSongs = { selectedItem = "Saved songs" },
                onBackToLibrary = { selectedItem = "Library" },
                onBackFromImport = { selectedItem = "Library" },
                onPlaySong = { uri, song ->
                    val service = activity.getPlaybackService()
                    if (service != null) {
                        service.playSong(uri, song)
                        playingSong = uri to song
                        isPlaying = true
                        // Start the service as foreground if not already
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(Intent(context, MediaPlaybackService::class.java))
                        } else {
                            context.startService(Intent(context, MediaPlaybackService::class.java))
                        }
                    }
                },
                onOpenImportSongs = { selectedItem = "Import songs" },
                onOpenCreatePlaylist = { selectedItem = "Create playlist" },
                playlists = playlists,
                onSavePlaylist = { name, songs ->
                    playlists = playlists + Playlist(name, songs)
                    selectedItem = "Library"
                },
                onChooseGoogleDriveFiles = {
                    chooseGoogleDriveFiles.launch(arrayOf("audio/*", "application/octet-stream"))
                }
            )
        }
        if (playingSong != null) {
            MiniPlayer(playingSong!!.first, playingSong!!.second, isPlaying, onOpen = { selectedItem = "Saved songs" }) {
                val service = activity.getPlaybackService()
                if (service != null) {
                    if (service.isCurrentlyPlaying()) {
                        service.pausePlayback()
                        isPlaying = false
                    } else {
                        service.resumePlayback()
                        isPlaying = true
                    }
                }
            }
        }
        BottomNavigationBar(selectedItem) { selectedItem = it }
    }
}

@Composable
private fun BottomNavigationBar(selectedItem: String, onItemSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().background(Color(0xFF100E13)).padding(horizontal = 20.dp, vertical = 10.dp),
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
    importedSongs: List<Uri>,
    downloadedSongs: Set<Uri>,
    onChooseAudioFiles: () -> Unit,
    onOpenSavedSongs: () -> Unit,
    onBackToLibrary: () -> Unit,
    onBackFromImport: () -> Unit,
    onPlaySong: (Uri, String) -> Unit,
    onOpenImportSongs: () -> Unit,
    onOpenCreatePlaylist: () -> Unit,
    playlists: List<Playlist>,
    onSavePlaylist: (String, Set<Uri>) -> Unit,
    onChooseGoogleDriveFiles: () -> Unit
) {
    if (selectedItem == "Import songs") {
        ImportSongsContent(
            onBack = onBackFromImport,
            importedSongs = importedSongs,
            downloadedSongs = downloadedSongs,
            onChooseAudioFiles = onChooseAudioFiles,
            onChooseGoogleDriveFiles = onChooseGoogleDriveFiles
        )
        return
    }

    when (selectedItem) {
        "Library" -> LibraryContent(importedSongs, onChooseAudioFiles, onOpenSavedSongs, playlists, onOpenCreatePlaylist)
        "Saved songs" -> SavedSongsContent(importedSongs, onChooseAudioFiles, onBackToLibrary, onPlaySong, onOpenImportSongs)
        "Create playlist" -> CreatePlaylistContent(importedSongs, downloadedSongs, onSavePlaylist, onBackToLibrary)
        else -> HomeContent(onOpenSavedSongs, importedSongs, playlists)
    }
}

@Composable
private fun HomeContent(
    onOpenSavedSongs: () -> Unit,
    importedSongs: List<Uri>,
    playlists: List<Playlist>
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("MusicCafe", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
private fun LibraryContent(
    importedSongs: List<Uri>,
    onChooseAudioFiles: () -> Unit,
    onOpenSavedSongs: () -> Unit,
    playlists: List<Playlist>,
    onOpenCreatePlaylist: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Library", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
            ImportSourceRow("Google Drive", onChooseGoogleDriveFiles)
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

private fun persistReadPermission(context: android.content.Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
    }
}

fun isSupportedAudioMimeType(mimeType: String?): Boolean {
    if (mimeType.isNullOrBlank()) return false
    val normalized = mimeType.lowercase()
    return normalized.startsWith("audio/") ||
        normalized.endsWith(".mp3") ||
        normalized.endsWith(".wav") ||
        normalized.endsWith(".m4a") ||
        normalized.endsWith(".aac") ||
        normalized.endsWith(".ogg")
}

fun sanitizeFileName(fileName: String): String {
    val trimmed = fileName.trim()
    val safe = trimmed.replace(Regex("[^A-Za-z0-9._-]"), "_")
    return if (safe.isBlank()) "audio_file" else safe
}

private fun isSupportedAudioUri(contentResolver: android.content.ContentResolver, uri: Uri): Boolean {
    val mimeType = contentResolver.getType(uri)
    return isSupportedAudioMimeType(mimeType)
}

private fun copyUriToAppStorage(context: android.content.Context, uri: Uri): Uri? {
    return try {
        val originalName = displayName(context.contentResolver, uri)
        val safeFileName = sanitizeFileName(originalName)
        val destination = java.io.File(context.filesDir, safeFileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        Uri.fromFile(destination)
    } catch (_: Exception) {
        null
    }
}

private fun downloadAudioFile(context: android.content.Context, uri: Uri): Boolean {
    return copyUriToAppStorage(context, uri) != null
}

@Preview(showBackground = true, widthDp = 900, heightDp = 800)
@Composable
private fun MusicCafePreview() {
    MusicCafeTheme(dynamicColor = false, darkTheme = true) {
        // Preview doesn't have access to MainActivity, so we use empty/placeholder
        // In production, MusicCafeApp is called from MainActivity with this@MainActivity
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
    onPlaySong: (Uri, String) -> Unit,
    onOpenImportSongs: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val trackData = remember(importedSongs) {
        importedSongs.map { uri ->
            val metadata = loadTrackMetadata(context, uri)
            metadata
        }
    }
    val visibleSongs = trackData.filter { track ->
        searchQuery.isBlank() || track.title.contains(searchQuery, ignoreCase = true) || track.artist.contains(searchQuery, ignoreCase = true)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F0E13)).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
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
        items(visibleSongs) { track ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onPlaySong(track.uri, track.title) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(74.dp).background(Color(0xFF3C3A40), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.artwork != null) {
                        Image(
                            bitmap = track.artwork.asImageBitmap(),
                            contentDescription = "Album art",
                            modifier = Modifier.fillMaxSize().background(Color(0xFFB9B4B8), RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFFB9B4B8), RoundedCornerShape(12.dp))
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Text(track.title, color = Color.White, fontSize = 18.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                    Text(track.artist, color = SoftText, fontSize = 15.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                }

                Text("••", color = SoftText, fontSize = 22.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

private data class TrackMetadata(
    val uri: Uri,
    val title: String,
    val artist: String,
    val artwork: Bitmap?
)

@Composable
private fun MiniPlayer(
    songUri: Uri,
    songTitle: String,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onTogglePlaying: () -> Unit
) {
    val context = LocalContext.current
    val track = remember(songUri) { loadTrackMetadata(context, songUri) }
    val displayTitle = if (songTitle.isNotBlank()) songTitle else track.title
    val displayArtist = track.artist
    val albumArt = track.artwork

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 5.dp)
            .background(Color(0xFF4D4A50), RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(start = 6.dp, end = 9.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (albumArt != null) {
            Image(
                bitmap = albumArt.asImageBitmap(),
                contentDescription = "Album artwork",
                modifier = Modifier.size(48.dp).background(Color(0xFFB9B4B8), RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFB9B4B8), RoundedCornerShape(12.dp))
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 9.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = displayTitle,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = displayArtist,
                color = SoftText,
                fontSize = 13.sp,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onTogglePlaying,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun loadTrackMetadata(context: android.content.Context, uri: Uri): TrackMetadata {
    return try {
        val retriever = MediaMetadataRetriever()
        try {
            if (uri.scheme == "content" || uri.scheme == "android.resource") {
                retriever.setDataSource(context, uri)
            } else if (uri.scheme == "file") {
                retriever.setDataSource(uri.path)
            } else {
                val savedUri = copyUriToAppStorage(context, uri)
                if (savedUri == null) return TrackMetadata(
                    uri = uri,
                    title = displayName(context.contentResolver, uri),
                    artist = "Cosmograph",
                    artwork = null
                )
                retriever.setDataSource(context, savedUri)
            }

            val embedded = retriever.embeddedPicture
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: displayName(context.contentResolver, uri)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "Cosmograph"
            val artwork = embedded?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            TrackMetadata(uri, title, artist, artwork)
        } finally {
            retriever.release()
        }
    } catch (_: Exception) {
        TrackMetadata(
            uri = uri,
            title = displayName(context.contentResolver, uri),
            artist = "Cosmograph",
            artwork = loadAlbumArt(context, uri)
        )
    }
}

private fun loadAlbumArt(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        val retriever = MediaMetadataRetriever()
        try {
            if (uri.scheme == "content" || uri.scheme == "android.resource") {
                retriever.setDataSource(context, uri)
            } else if (uri.scheme == "file") {
                retriever.setDataSource(uri.path)
            } else {
                val savedUri = copyUriToAppStorage(context, uri)
                if (savedUri == null) return null
                retriever.setDataSource(context, savedUri)
            }

            val embedded = retriever.embeddedPicture ?: return null
            BitmapFactory.decodeByteArray(embedded, 0, embedded.size)
        } finally {
            retriever.release()
        }
    } catch (_: Exception) {
        null
    }
}