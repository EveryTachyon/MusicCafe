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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musiccafe.displayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext

@Composable
fun ImportSongsContent(
    onBack: () -> Unit,
    importedSongs: List<Uri>,
    downloadedSongs: Set<Uri>,
    onChooseAudioFiles: () -> Unit,
    onChooseGoogleDriveFiles: () -> Unit,
    onDownloadYoutube: (String) -> Unit,
    onDeleteAllDownloads: () -> Unit,
    isDownloading: Boolean,
    downloadError: String?
) {
    val context = LocalContext.current
    var youtubeUrl by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var songNames by remember { mutableStateOf(importedSongs.map { uri -> uri to "Audio file" }) }
    LaunchedEffect(importedSongs) {
        songNames = importedSongs.map { uri ->
            uri to (uri.lastPathSegment?.substringAfterLast('/') ?: "Audio file")
        }
        importedSongs.chunked(6).forEach { batch ->
            val names = withContext(Dispatchers.IO) {
                batch.map { uri ->
                    async { uri to displayName(context.contentResolver, uri) }
                }.awaitAll()
            }
            val namesByUri = names.toMap()
            songNames = songNames.map { (uri, currentName) ->
                uri to (namesByUri[uri] ?: currentName)
            }
            yield()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 28.dp),
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
        item { ImportSourceRow("Device", onChooseAudioFiles) }
        item { ImportSourceRow("Google Drive", onChooseGoogleDriveFiles) }
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
        if (importedSongs.isNotEmpty()) {
            item {
                Text(
                    text = "Music",
                    color = AccentGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(songNames, key = { (uri, _) -> uri.toString() }) { (uri, songName) ->
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
                    Checkbox(
                        checked = uri in downloadedSongs,
                        onCheckedChange = null
                    )
                }
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
fun ImportSourceRow(name: String, onClick: () -> Unit) {
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
