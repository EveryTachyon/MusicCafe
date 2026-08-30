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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musiccafe.displayName

@Composable
fun ImportSongsContent(
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
                    Checkbox(
                        checked = uri in downloadedSongs,
                        onCheckedChange = null
                    )
                }
            }
        }
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
