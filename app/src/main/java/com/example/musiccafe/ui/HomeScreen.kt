package com.example.musiccafe.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musiccafe.Playlist
import com.example.musiccafe.displayName

@Composable
fun HomeContent(
    onOpenSavedSongs: () -> Unit,
    importedSongs: List<Uri>,
    playlists: List<Playlist>,
    onOpenPlaylist: (Playlist) -> Unit,
    recentlyPlayed: List<Uri>,
    onPlaySong: (Uri, String) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "MusicCafe",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Text(
                    "Recently played",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 22.dp)
                )
            }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recentlyPlayed, key = { it.toString() }) { uri ->
                        val title = remember(uri) {
                            displayName(context.contentResolver, uri)
                        }
                        RecentlyPlayedCard(
                            title = title,
                            artworkUri = uri,
                            onClick = { onPlaySong(uri, title) }
                        )
                    }
                }
            }
        }
        if (importedSongs.isNotEmpty() || playlists.isNotEmpty()) {
            item {
                Text(
                    "Saved songs",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 22.dp)
                )
            }

            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (importedSongs.isNotEmpty()) {
                        item {
                            HomeCollectionCard(
                                title = "Saved songs",
                                onClick = onOpenSavedSongs
                            )
                        }
                    }
                    items(playlists, key = { it.name }) { playlist ->
                        HomeCollectionCard(
                            title = playlist.name,
                            artworkUri = playlist.songs.firstOrNull(),
                            onClick = { onOpenPlaylist(playlist) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedCard(
    title: String,
    artworkUri: Uri,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(156.dp)
            .background(CardBackground, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        SongArtwork(
            uri = artworkUri,
            modifier = Modifier.fillMaxWidth().height(104.dp),
            cornerRadius = 12.dp
        )
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
        )
    }
}

@Composable
fun HomeCollectionCard(title: String, artworkUri: Uri? = null, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .width(250.dp)
            .height(86.dp)
            .background(CardBackground, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (artworkUri != null) {
            SongArtwork(artworkUri, Modifier.width(96.dp).fillMaxHeight(), 12.dp)
        } else {
            Box(
                modifier = Modifier.width(96.dp).fillMaxHeight().background(Color(0xFF1F1D23), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("♪", color = SoftText, fontSize = 30.sp)
            }
        }
        Text(
            title,
            color = Color.White,
            fontSize = 17.sp,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
    }
}
