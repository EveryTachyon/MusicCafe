package com.example.musiccafe

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle

/**
 * Foreground service for managing music playback with media notifications.
 * Handles playback control actions (play, pause, skip) from the notification.
 */
class MediaPlaybackService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "music_playback"
        private const val ACTION_PLAY = "com.example.musiccafe.ACTION_PLAY"
        private const val ACTION_PAUSE = "com.example.musiccafe.ACTION_PAUSE"
        private const val ACTION_NEXT = "com.example.musiccafe.ACTION_NEXT"
        private const val ACTION_PREV = "com.example.musiccafe.ACTION_PREV"
    }
    
    private val binder = MediaPlaybackBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var currentSong: Pair<Uri, String>? = null
    private var isPlaying = false
    private var notificationManager: NotificationManager? = null
    private var playbackListener: PlaybackListener? = null
    
    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let { action ->
                when (action) {
                    ACTION_PLAY -> resumePlayback()
                    ACTION_PAUSE -> pausePlayback()
                    ACTION_NEXT -> notifySkipNext()
                    ACTION_PREV -> notifySkipPrev()
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        registerBroadcastReceiver()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent): IBinder = binder
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterBroadcastReceiver()
        stopPlayback()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
    
    /**
     * Start playing a song and show notification
     */
    fun playSong(uri: Uri, title: String) {
        stopPlayback()
        currentSong = uri to title
        
        mediaPlayer = try {
            MediaPlayer.create(this, uri)?.also { player ->
                player.setOnCompletionListener {
                    isPlaying = false
                    playbackListener?.onSongCompleted()
                    updateNotification()
                }
                player.start()
                isPlaying = true
            }
        } catch (e: Exception) {
            null
        }
        
        updateNotification()
    }
    
    /**
     * Resume playback
     */
    fun resumePlayback() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                isPlaying = true
                playbackListener?.onPlaybackStateChanged(true)
                updateNotification()
            }
        }
    }
    
    /**
     * Pause playback
     */
    fun pausePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                playbackListener?.onPlaybackStateChanged(false)
                updateNotification()
            }
        }
    }
    
    /**
     * Stop playback and release resources
     */
    fun stopPlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause()
            it.release()
        }
        mediaPlayer = null
        isPlaying = false
        currentSong = null
    }
    
    /**
     * Get current playback state
     */
    fun isCurrentlyPlaying(): Boolean = isPlaying
    
    /**
     * Get current song information
     */
    fun getCurrentSong(): Pair<Uri, String>? = currentSong
    
    /**
     * Set playback listener for UI updates
     */
    fun setPlaybackListener(listener: PlaybackListener) {
        this.playbackListener = listener
    }
    
    /**
     * Notify skip next action
     */
    private fun notifySkipNext() {
        playbackListener?.onSkipNext()
    }
    
    /**
     * Notify skip previous action
     */
    private fun notifySkipPrev() {
        playbackListener?.onSkipPrev()
    }
    
    /**
     * Create notification channel for Android 8+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Shows currently playing music with controls"
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    /**
     * Update or create the media notification
     */
    private fun updateNotification() {
        val (uri, title) = currentSong ?: return
        
        val notification = buildMediaNotification(title)
        startForeground(NOTIFICATION_ID, notification)
    }
    
    /**
     * Build the media notification with playback controls
     */
    private fun buildMediaNotification(songTitle: String): Notification {
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
        
        val prevAction = NotificationCompat.Action(
            android.R.drawable.ic_media_previous,
            "Previous",
            createBroadcastPendingIntent(ACTION_PREV)
        )
        
        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next,
            "Next",
            createBroadcastPendingIntent(ACTION_NEXT)
        )
        
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(songTitle)
            .setContentText("Now Playing")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openAppPendingIntent)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(MediaStyle()
                .setShowActionsInCompactView(0, 1, 2))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)
            .build()
    }
    
    /**
     * Create a pending intent for broadcast receiver
     */
    private fun createBroadcastPendingIntent(action: String): PendingIntent {
        val intent = Intent(action).setPackage(packageName)
        return PendingIntent.getBroadcast(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Register broadcast receiver for control actions
     */
    private fun registerBroadcastReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREV)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(controlReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(controlReceiver, filter)
        }
    }
    
    /**
     * Unregister broadcast receiver
     */
    private fun unregisterBroadcastReceiver() {
        try {
            unregisterReceiver(controlReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }
    
    /**
     * Binder for binding to this service
     */
    inner class MediaPlaybackBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }
    
    /**
     * Listener for playback events
     */
    interface PlaybackListener {
        fun onPlaybackStateChanged(isPlaying: Boolean)
        fun onSkipNext()
        fun onSkipPrev()
        fun onSongCompleted()
    }
}
