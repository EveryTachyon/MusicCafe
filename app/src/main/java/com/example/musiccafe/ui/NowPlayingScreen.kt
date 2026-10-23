package com.example.musiccafe.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musiccafe.MainActivity
import com.example.musiccafe.MediaPlaybackService
import com.example.musiccafe.loadTrackMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    val context = LocalContext.current
    var position by remember(songUri) { mutableIntStateOf(0) }
    var duration by remember(songUri) { mutableIntStateOf(0) }
    var sliderPosition by remember(songUri) { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var shuffleEnabled by remember(songUri) { mutableStateOf(service?.isShuffleEnabled() ?: false) }
    var repeatMode by remember(songUri) {
        mutableIntStateOf(service?.getRepeatMode() ?: MediaPlaybackService.REPEAT_OFF)
    }
    val track = remember(songUri) { loadTrackMetadata(context, songUri) }

    LaunchedEffect(songUri, service, isSeeking, isPlaying) {
        if (isSeeking) return@LaunchedEffect
        while (isActive) {
            position = service?.getPlaybackPosition() ?: 0
            duration = service?.getPlaybackDuration() ?: 0
            sliderPosition = if (duration > 0) position.toFloat() / duration else 0f
            onPlayingChanged(service?.isCurrentlyPlaying() ?: false)
            delay(250)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ContentBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Minimize player",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Now playing", color = Color.White, fontSize = 20.sp)
                Text("Saved songs", color = AccentGreen, fontSize = 16.sp)
            }
            Spacer(Modifier.width(48.dp))
        }

        SongArtwork(songUri, Modifier.fillMaxWidth().aspectRatio(1f), 12.dp)

        Text(
            text = songTitle,
            color = Color.White,
            fontSize = 25.sp,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp)
        )
        Text(
            text = track.artist,
            color = SoftText,
            fontSize = 19.sp,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )

        Spacer(Modifier.size(22.dp))
        Slider(
            value = sliderPosition.coerceIn(0f, 1f),
            enabled = duration > 0,
            onValueChange = { value ->
                isSeeking = true
                sliderPosition = value
            },
            onValueChangeFinished = {
                service?.seekTo((sliderPosition * duration).toInt())
                isSeeking = false
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = AccentGreen,
                activeTrackColor = AccentGreen,
                inactiveTrackColor = CardBackground
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position), color = SoftText, fontSize = 13.sp)
            Text(formatTime(duration), color = SoftText, fontSize = 13.sp)
        }

        Spacer(Modifier.size(34.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    shuffleEnabled = !shuffleEnabled
                    service?.setShuffleEnabled(shuffleEnabled)
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Outlined.Shuffle,
                    contentDescription = if (shuffleEnabled) "Turn shuffle off" else "Turn shuffle on",
                    tint = if (shuffleEnabled) AccentGreen else SoftText,
                    modifier = Modifier.size(25.dp)
                )
            }
            IconButton(
                onClick = {
                    if (position > 3_000) service?.seekTo(0) else service?.skipToPrevious()
                },
                modifier = Modifier.size(54.dp)
            ) {
                Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous song", tint = Color.White, modifier = Modifier.size(34.dp))
            }
            IconButton(
                onClick = {
                    if (service?.isCurrentlyPlaying() == true) {
                        service.pausePlayback()
                        onPlayingChanged(false)
                    } else {
                        service?.resumePlayback()
                        onPlayingChanged(true)
                    }
                },
                modifier = Modifier.size(76.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
            IconButton(onClick = { service?.skipToNext() }, modifier = Modifier.size(54.dp)) {
                Icon(Icons.Outlined.SkipNext, contentDescription = "Next song", tint = Color.White, modifier = Modifier.size(34.dp))
            }
            IconButton(
                onClick = { repeatMode = service?.cycleRepeatMode() ?: repeatMode },
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Repeat,
                        contentDescription = when (repeatMode) {
                            MediaPlaybackService.REPEAT_ALL -> "Repeat all"
                            MediaPlaybackService.REPEAT_ONE -> "Repeat one"
                            else -> "Turn repeat on"
                        },
                        tint = if (repeatMode == MediaPlaybackService.REPEAT_OFF) SoftText else AccentGreen,
                        modifier = Modifier.size(25.dp)
                    )
                    if (repeatMode == MediaPlaybackService.REPEAT_ONE) {
                        Text("1", color = AccentGreen, fontSize = 10.sp)
                    }
                }
            }
        }
        Spacer(Modifier.size(24.dp))
    }
}

private fun formatTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
