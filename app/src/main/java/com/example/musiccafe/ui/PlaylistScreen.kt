package com.example.musiccafe.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musiccafe.displayName

@Composable
fun CreatePlaylistContent(
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
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Create playlist", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
        BasicTextField(
            value = playlistName,
            onValueChange = { playlistName = it },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth().background(CardBackground, RoundedCornerShape(12.dp)).padding(18.dp)) {
                    if (playlistName.isBlank()) Text("Playlist name", color = SoftText, fontSize = 20.sp)
                    innerTextField()
                }
            }
        )
        Text(
            "Choose songs",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 26.dp, bottom = 8.dp)
        )
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(songNames) { (uri, songName) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(8.dp))
                        .clickable {
                            selectedSongs = if (uri in selectedSongs) selectedSongs - uri else selectedSongs + uri
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
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
            modifier = Modifier
                .fillMaxWidth()
                .background(AccentGreen, RoundedCornerShape(28.dp))
                .clickable(enabled = playlistName.isNotBlank()) {
                    onSavePlaylist(playlistName.trim(), selectedSongs)
                }
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("SAVE PLAYLIST", color = Color(0xFF071014), fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}
