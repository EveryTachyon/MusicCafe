package com.example.musiccafe.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musiccafe.MainActivity
import com.example.musiccafe.Playlist
import com.example.musiccafe.copyUriToAppStorage
import com.example.musiccafe.isSupportedAudioUri
import com.example.musiccafe.loadDownloadedSongs
import com.example.musiccafe.loadPlaylists
import com.example.musiccafe.downloadYoutubeAudio
import com.example.musiccafe.deleteDownloadedSong
import com.example.musiccafe.deleteAllDownloadedSongs
import com.example.musiccafe.loadImportedSongs
import com.example.musiccafe.persistReadPermission
import com.example.musiccafe.saveDownloadedSongs
import com.example.musiccafe.savePlaylists
import kotlinx.coroutines.launch
import com.example.musiccafe.saveImportedSongs

@Composable
fun MusicCafeApp(activity: MainActivity) {
    val context = LocalContext.current
    var selectedItem by remember { mutableStateOf("Home") }
    var isImportScreenOpen by remember { mutableStateOf(false) }
    var importedSongs by remember { mutableStateOf(loadImportedSongs(context)) }
    var playlists by remember { mutableStateOf(loadPlaylists(context)) }
    var playingSong by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var downloadedSongs by remember { mutableStateOf(loadDownloadedSongs(context)) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(importedSongs) {
        saveImportedSongs(context, importedSongs)
    }

    LaunchedEffect(downloadedSongs) {
        saveDownloadedSongs(context, downloadedSongs)
    }

    LaunchedEffect(playlists) {
        savePlaylists(context, playlists)
    }

    val chooseAudioFiles = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { audioUris ->
        val validAudioUris = audioUris.filter { uri -> isSupportedAudioUri(context.contentResolver, uri) }
        validAudioUris.forEach { uri -> persistReadPermission(context, uri) }
        importedSongs = (importedSongs + validAudioUris).distinct()
        isImportScreenOpen = true
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
        isImportScreenOpen = true
    }

    Column(modifier = Modifier.fillMaxSize().background(ContentBackground)) {
        Box(modifier = Modifier.weight(1f)) {
            LandingContent(
                selectedItem = selectedItem,
                isImportScreenOpen = isImportScreenOpen,
                importedSongs = importedSongs,
                downloadedSongs = downloadedSongs,
                onChooseAudioFiles = {
                    isImportScreenOpen = true
                    chooseAudioFiles.launch(arrayOf("audio/*"))
                },
                onOpenSavedSongs = {
                    selectedItem = "Saved songs"
                    isImportScreenOpen = false
                },
                onBackToLibrary = {
                    selectedItem = "Library"
                    isImportScreenOpen = false
                },
                onBackFromImport = {
                    selectedItem = "Library"
                    isImportScreenOpen = false
                },
                onPlaySong = { uri, song ->
                    val service = activity.getPlaybackService()
                    if (service != null) {
                        service.playSong(uri, song)
                        playingSong = uri to song
                        isPlaying = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(Intent(context, com.example.musiccafe.MediaPlaybackService::class.java))
                        } else {
                            context.startService(Intent(context, com.example.musiccafe.MediaPlaybackService::class.java))
                        }
                    }
                },
                onDeleteSong = { uri ->
                    if (uri in downloadedSongs && !deleteDownloadedSong(uri)) return@LandingContent
                    importedSongs = importedSongs.filterNot { it == uri }
                    downloadedSongs = downloadedSongs - uri
                    playlists = playlists.map { playlist ->
                        playlist.copy(songs = playlist.songs - uri)
                    }
                    if (playingSong?.first == uri) {
                        activity.getPlaybackService()?.stopPlayback()
                        playingSong = null
                        isPlaying = false
                    }
                },
                onDeleteAllDownloads = {
                    val deletedSongs = deleteAllDownloadedSongs(downloadedSongs)
                    importedSongs = importedSongs.filterNot { it in deletedSongs }
                    downloadedSongs = downloadedSongs - deletedSongs
                    playlists = playlists.map { playlist ->
                        playlist.copy(songs = playlist.songs - deletedSongs)
                    }
                    if (playingSong?.first in deletedSongs) {
                        activity.getPlaybackService()?.stopPlayback()
                        playingSong = null
                        isPlaying = false
                    }
                },
                onOpenImportSongs = {
                    selectedItem = "Import songs"
                    isImportScreenOpen = true
                },
                onOpenCreatePlaylist = {
                    selectedItem = "Create playlist"
                    isImportScreenOpen = false
                },
                playlists = playlists,
                onSavePlaylist = { name, songs ->
                    playlists = playlists + Playlist(name, songs)
                    selectedItem = "Library"
                    isImportScreenOpen = false
                },
                onChooseGoogleDriveFiles = {
                    isImportScreenOpen = true
                    chooseGoogleDriveFiles.launch(arrayOf("audio/*", "application/octet-stream"))
                },
                onDownloadYoutube = { url ->
                    coroutineScope.launch {
                        isDownloading = true
                        downloadError = null
                        runCatching { downloadYoutubeAudio(context, url) }
                            .onSuccess { track ->
                                importedSongs = (importedSongs + track.uri).distinct()
                                downloadedSongs = downloadedSongs + track.uri
                                val youtubePlaylist = playlists.firstOrNull { it.name == "YouTube Downloads" }
                                playlists = if (youtubePlaylist == null) {
                                    playlists + Playlist("YouTube Downloads", setOf(track.uri))
                                } else {
                                    playlists.map { playlist ->
                                        if (playlist.name == "YouTube Downloads") {
                                            playlist.copy(songs = playlist.songs + track.uri)
                                        } else {
                                            playlist
                                        }
                                    }
                                }
                            }
                            .onFailure {
                                Log.e("MusicCafeDownload", "YouTube download failed", it)
                                downloadError = when {
                                    it.message?.contains("video link", ignoreCase = true) == true -> it.message
                                    it.message?.contains("no audio", ignoreCase = true) == true -> "This video has no downloadable audio."
                                    it.message?.contains("URL not accepted", ignoreCase = true) == true -> "YouTube rejected this link. Try a public video link."
                                    else -> "Could not download this video. Check the link and your internet connection."
                                }
                            }
                        isDownloading = false
                    }
                },
                isDownloading = isDownloading,
                downloadError = downloadError
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
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(SidebarBackground)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        BottomNavigationItem("Home", Icons.Outlined.Home, selectedItem, onItemSelected)
        BottomNavigationItem("Library", Icons.Outlined.LibraryMusic, selectedItem, onItemSelected)
    }
}

@Composable
private fun BottomNavigationItem(
    label: String,
    icon: ImageVector,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.width(76.dp).clickable { onItemSelected(label) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selectedItem == label) AccentGreen else SidebarText,
            modifier = Modifier.height(30.dp)
        )
        Text(label, color = if (selectedItem == label) AccentGreen else SidebarText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LandingContent(
    selectedItem: String,
    isImportScreenOpen: Boolean,
    importedSongs: List<Uri>,
    downloadedSongs: Set<Uri>,
    onChooseAudioFiles: () -> Unit,
    onOpenSavedSongs: () -> Unit,
    onBackToLibrary: () -> Unit,
    onBackFromImport: () -> Unit,
    onPlaySong: (Uri, String) -> Unit,
    onDeleteSong: (Uri) -> Unit,
    onDeleteAllDownloads: () -> Unit,
    onOpenImportSongs: () -> Unit,
    onOpenCreatePlaylist: () -> Unit,
    playlists: List<Playlist>,
    onSavePlaylist: (String, Set<Uri>) -> Unit,
    onChooseGoogleDriveFiles: () -> Unit,
    onDownloadYoutube: (String) -> Unit,
    isDownloading: Boolean,
    downloadError: String?
) {
    if (isImportScreenOpen) {
        ImportSongsContent(
            onBack = onBackFromImport,
            importedSongs = importedSongs,
            downloadedSongs = downloadedSongs,
            onChooseAudioFiles = onChooseAudioFiles,
            onChooseGoogleDriveFiles = onChooseGoogleDriveFiles,
            onDownloadYoutube = onDownloadYoutube,
            onDeleteAllDownloads = onDeleteAllDownloads,
            isDownloading = isDownloading,
            downloadError = downloadError
        )
        return
    }

    when (selectedItem) {
        "Library" -> LibraryContent(importedSongs, onOpenSavedSongs, playlists, onOpenCreatePlaylist)
        "Saved songs" -> SavedSongsContent(
            importedSongs,
            downloadedSongs,
            onBackToLibrary,
            onPlaySong,
            onOpenImportSongs,
            onDeleteSong,
            onDeleteAllDownloads
        )
        "Create playlist" -> CreatePlaylistContent(importedSongs, downloadedSongs, onSavePlaylist, onBackToLibrary)
        else -> HomeContent(onOpenSavedSongs, importedSongs, playlists)
    }
}
