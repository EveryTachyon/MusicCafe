package com.example.musiccafe.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musiccafe.DeviceAudioFile
import com.example.musiccafe.queryDeviceAudioFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ImportSongsContent(
    onBack: () -> Unit,
    importedSongs: List<Uri>,
    downloadedSongs: Set<Uri>,
    onChooseAudioFiles: () -> Unit,
    isDevicePickerOpen: Boolean,
    onCloseDevicePicker: () -> Unit,
    onAddDeviceSong: (Uri) -> Unit,
    onChooseGoogleDriveFiles: () -> Unit,
    onDownloadYoutube: (String) -> Unit,
    onDeleteAllDownloads: () -> Unit,
    isDownloading: Boolean,
    downloadError: String?
) {
    val context = LocalContext.current
    if (isDevicePickerOpen) {
        DeviceAudioPickerContent(
            onBack = onCloseDevicePicker,
            importedSongs = importedSongs,
            onAddSong = onAddDeviceSong
        )
        return
    }
    var youtubeUrl by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
        item {
            ImportSourceRow(
                name = "This device",
                icon = Icons.Outlined.AudioFile,
                onClick = onChooseAudioFiles
            )
        }
        item {
            ImportSourceRow(
                name = "Google Drive",
                icon = Icons.Outlined.Cloud,
                onClick = onChooseGoogleDriveFiles
            )
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().background(CardBackground, RoundedCornerShape(16.dp)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("YouTube", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                BasicTextField(
                    value = youtubeUrl,
                    onValueChange = { youtubeUrl = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF121212), RoundedCornerShape(8.dp)).padding(14.dp),
                    decorationBox = { innerTextField ->
                        if (youtubeUrl.isBlank()) Text("Paste a YouTube link", color = SoftText, fontSize = 16.sp)
                        innerTextField()
                    }
                )
                Text(
                    text = if (isDownloading) "Downloading..." else "DOWNLOAD",
                    color = if (youtubeUrl.isBlank() || isDownloading) SoftText else AccentGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = youtubeUrl.isNotBlank() && !isDownloading) {
                        onDownloadYoutube(youtubeUrl.trim())
                    }
                )
                downloadError?.let { Text(it, color = Color(0xFFFF8A80), fontSize = 14.sp) }
                Text(
                    text = "DELETE DOWNLOADED FILES (${downloadedSongs.size})",
                    color = if (downloadedSongs.isEmpty()) SoftText else Color(0xFFFF8A80),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = downloadedSongs.isNotEmpty()) {
                        showDeleteDialog = true
                    }
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete downloaded files?") },
            text = { Text("This removes ${downloadedSongs.size} files downloaded or copied into MusicCafe.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteAllDownloads()
                }) { Text("DELETE") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
private fun DeviceAudioPickerContent(
    onBack: () -> Unit,
    importedSongs: List<Uri>,
    onAddSong: (Uri) -> Unit
) {
    val context = LocalContext.current
    var songs by remember { mutableStateOf<List<DeviceAudioFile>>(emptyList()) }
    var searchText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        songs = withContext(Dispatchers.IO) {
            queryDeviceAudioFiles(context).map { song ->
                song.copy(artwork = com.example.musiccafe.loadTrackMetadata(context, song.uri).artwork)
            }
        }
    }

    val uniqueSongs = songs.distinctBy { song ->
        "${song.title.trim().lowercase()}|${song.artist.trim().lowercase()}"
    }
    val filteredSongs = uniqueSongs.filter { song ->
        val isImported = song.uri in importedSongs
        val matchesFilter = when (selectedFilter) {
            "Added" -> isImported
            "Not added" -> !isImported
            else -> true
        }
        matchesFilter && (
            searchText.isBlank() ||
                song.title.contains(searchText, ignoreCase = true) ||
                song.artist.contains(searchText, ignoreCase = true)
            )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 40.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Choose songs", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Text("${importedSongs.size} already added", color = AccentGreen, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Search, null, tint = SoftText)
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = searchText,
                onValueChange = { searchText = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (searchText.isBlank()) Text("Search songs or artists", color = SoftText)
                    innerTextField()
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Not added", "Added").forEach { filter ->
                Text(
                    text = filter,
                    color = if (selectedFilter == filter) Color.Black else SoftText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(
                            if (selectedFilter == filter) AccentGreen else CardBackground,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (uniqueSongs.isEmpty()) {
            Text(
                "No songs found on this device.",
                color = SoftText,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredSongs, key = { it.uri.toString() }) { song ->
                    val isImported = song.uri in importedSongs
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(
                                if (isImported) Color(0xFF102316) else CardBackground,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (!isImported) {
                                    onAddSong(song.uri)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (song.artwork != null) {
                            androidx.compose.foundation.Image(
                                bitmap = song.artwork.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Album,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(56.dp).padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                text = song.title,
                                color = Color.White,
                                fontSize = 17.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${song.artist}  •  ${formatDuration(song.durationMs)}",
                                color = SoftText,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = if (isImported) "Already added" else "Add",
                            color = if (isImported) SoftText else AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
fun ImportSourceRow(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .background(CardBackground, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier.width(34.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}
