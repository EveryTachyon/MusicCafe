package com.example.musiccafe.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.musiccafe.MainActivity

class MediaNotification(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "music_playback"

        private const val CHANNEL_NAME = "Music Playback"
        private const val ACTION_PLAY = "com.example.musiccafe.ACTION_PLAY"
        private const val ACTION_PAUSE = "com.example.musiccafe.ACTION_PAUSE"
        private const val ACTION_NEXT = "com.example.musiccafe.ACTION_NEXT"
        private const val ACTION_PREV = "com.example.musiccafe.ACTION_PREV"
    }

    private val mediaSession = MediaSessionCompat(context, "MusicCafe").apply {
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() = sendControlAction(ACTION_PLAY)

            override fun onPause() = sendControlAction(ACTION_PAUSE)

            override fun onSkipToNext() = sendControlAction(ACTION_NEXT)

            override fun onSkipToPrevious() = sendControlAction(ACTION_PREV)
        })
        isActive = true
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows currently playing music with controls"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun build(
        songTitle: String,
        artist: String,
        artwork: Bitmap?,
        isPlaying: Boolean,
        position: Int,
        duration: Int
    ): Notification {
        val metadataBuilder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, songTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration.toLong())
        // Clear cached artwork when the next track has no embedded image.
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artwork)
        mediaSession.setMetadata(metadataBuilder.build())
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING
                    else PlaybackStateCompat.STATE_PAUSED,
                    position.toLong(),
                    if (isPlaying) 1f else 0f
                )
                .build()
        )

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "Pause",
                createBroadcastPendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "Play",
                createBroadcastPendingIntent(ACTION_PLAY)
            )
        }

        val previousAction = NotificationCompat.Action(
            android.R.drawable.ic_media_previous,
            "Previous",
            createBroadcastPendingIntent(ACTION_PREV)
        )
        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next,
            "Next",
            createBroadcastPendingIntent(ACTION_NEXT)
        )

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(songTitle)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openAppPendingIntent)
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)
            .apply {
                if (artwork != null) {
                    setLargeIcon(artwork)
                    setStyle(
                        MediaStyle()
                            .setMediaSession(mediaSession.sessionToken)
                            .setShowActionsInCompactView(0, 1, 2)
                    )
                }
            }

        return builder.build()
    }

    fun release() {
        mediaSession.isActive = false
        mediaSession.release()
    }

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun createBroadcastPendingIntent(action: String): PendingIntent {
        val intent = Intent(action).setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun sendControlAction(action: String) {
        context.sendBroadcast(Intent(action).setPackage(context.packageName))
    }
}
