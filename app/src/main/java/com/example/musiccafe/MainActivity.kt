package com.example.musiccafe

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.example.musiccafe.ui.MusicCafeApp
import com.example.musiccafe.ui.theme.MusicCafeTheme

class MainActivity : ComponentActivity(), MediaPlaybackService.PlaybackListener {
    private var playbackService: MediaPlaybackService? = null
    private var isBound = false
    private var playbackStateObserver: ((Pair<android.net.Uri, String>?, Boolean) -> Unit)? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MediaPlaybackService.MediaPlaybackBinder
            playbackService = binder.getService()
            playbackService?.setPlaybackListener(this@MainActivity)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        bindToService()
        setContent {
            MusicCafeTheme {
                MusicCafeApp(this@MainActivity)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun bindToService() {
        val intent = Intent(this, MediaPlaybackService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun getPlaybackService(): MediaPlaybackService? = playbackService

    fun observePlaybackState(observer: ((Pair<android.net.Uri, String>?, Boolean) -> Unit)?) {
        playbackStateObserver = observer
        observer?.invoke(playbackService?.getCurrentSong(), playbackService?.isCurrentlyPlaying() ?: false)
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        playbackStateObserver?.invoke(playbackService?.getCurrentSong(), isPlaying)
    }

    override fun onSongChanged(song: Pair<android.net.Uri, String>) {
        playbackStateObserver?.invoke(song, playbackService?.isCurrentlyPlaying() ?: false)
    }

    override fun onSkipNext() {
    }

    override fun onSkipPrev() {
    }

    override fun onSongCompleted() {
        playbackStateObserver?.invoke(playbackService?.getCurrentSong(), false)
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 100
    }
}
