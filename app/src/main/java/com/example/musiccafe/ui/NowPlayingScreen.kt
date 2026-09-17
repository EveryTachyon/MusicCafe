package com.example.musiccafe.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musiccafe.MainActivity
import kotlinx.coroutines.delay

@Composable
fun NowPlayingContent(
    activity: MainActivity,
    songUri: Uri,
    songTitle: String,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayingChanged: (Boolean) -> Unit
) {
    val service = activity.getPlaybackService()
    val context = androidx.compose.ui.platform.LocalContext.current
    var position by remember(songUri) { mutableIntStateOf(0) }
    var duration by remember(songUri) { mutableIntStateOf(0) }
    var sliderPosition by remember(songUri) { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    val track = remember(songUri) { com.example.musiccafe.loadTrackMetadata(context, songUri) }

    LaunchedEffect(songUri, service) {
        while (true) {
            if (!isSeeking) {
                position = service?.getPlaybackPosition() ?: 0
                duration = service?.getPlaybackDuration() ?: 0
                sliderPosition = if (duration > 0) position.toFloat() / duration else 0f
            }
            delay(500)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(ContentBackground).padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("NOW PLAYING", color = SoftText, fontSize = 14.sp, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.weight(0.7f))
        SongArtwork(songUri, Modifier.fillMaxWidth().size(330.dp), 12.dp)
        Text(songTitle, color = Color.White, fontSize = 23.sp, maxLines = 1, modifier = Modifier.padding(top = 24.dp))
        Text(track.artist, color = SoftText, fontSize = 16.sp, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.size(18.dp))
        Slider(
            value = sliderPosition,
            onValueChange = { value -> isSeeking = true; sliderPosition = value },
            onValueChangeFinished = {
                service?.seekTo((sliderPosition * duration).toInt())
                isSeeking = false
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position), color = SoftText, fontSize = 13.sp)
            Text(formatTime(duration), color = SoftText, fontSize = 13.sp)
        }
        Spacer(Modifier.size(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(30.dp)) {
            Icon(Icons.Outlined.Shuffle, contentDescription = "Shuffle", tint = SoftText, modifier = Modifier.size(24.dp))
            IconButton(onClick = { service?.seekTo((position - 10_000).coerceAtLeast(0)) }) {
                Text("-10", color = Color.White, fontSize = 14.sp)
            }
            IconButton(onClick = {
                if (service?.isCurrentlyPlaying() == true) {
                    service.pausePlayback()
                    onPlayingChanged(false)
                } else {
                    service?.resumePlayback()
                    onPlayingChanged(true)
                }
            }, modifier = Modifier.size(70.dp)) {
                Icon(if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = "Play or pause", tint = Color.White, modifier = Modifier.size(48.dp))
            }
            IconButton(onClick = { service?.seekTo((position + 10_000).coerceAtMost(duration)) }) {
                Text("+10", color = Color.White, fontSize = 14.sp)
            }
            Icon(Icons.Outlined.Repeat, contentDescription = "Repeat", tint = SoftText, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.weight(1f))
    }
}

private fun formatTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}