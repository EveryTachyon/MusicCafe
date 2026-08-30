package com.example.musiccafe

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

@Suppress("SpellCheckingInspection")
const val DEFAULT_ARTIST = "Cosmograph"

data class Playlist(val name: String, val songs: Set<Uri>)

data class TrackMetadata(
    val uri: Uri,
    val title: String,
    val artist: String,
    val artwork: Bitmap?
)

fun displayName(contentResolver: ContentResolver, uri: Uri): String {
    val nameColumn = OpenableColumns.DISPLAY_NAME
    contentResolver.query(uri, arrayOf(nameColumn), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndexOrThrow(nameColumn))
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "Audio file"
}

private const val APP_STORAGE_PREFS = "musiccafe_preferences"
private const val IMPORTED_SONGS_KEY = "imported_songs"
private const val DOWNLOADED_SONGS_KEY = "downloaded_songs"

fun persistReadPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
    }
}

fun saveImportedSongs(context: Context, songs: List<Uri>) {
    val prefs = context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
    prefs.edit().putStringSet(IMPORTED_SONGS_KEY, songs.map { it.toString() }.toSet()).apply()
}

fun loadImportedSongs(context: Context): List<Uri> {
    val prefs = context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
    return (prefs.getStringSet(IMPORTED_SONGS_KEY, emptySet()) ?: emptySet())
        .mapNotNull { uriString ->
            runCatching { Uri.parse(uriString) }.getOrNull()?.also { uri ->
                if (uri.scheme == "content") {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {
                    }
                }
            }
        }
}

fun saveDownloadedSongs(context: Context, songs: Set<Uri>) {
    val prefs = context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
    prefs.edit().putStringSet(DOWNLOADED_SONGS_KEY, songs.map { it.toString() }.toSet()).apply()
}

fun loadDownloadedSongs(context: Context): Set<Uri> {
    val prefs = context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
    return (prefs.getStringSet(DOWNLOADED_SONGS_KEY, emptySet()) ?: emptySet())
        .mapNotNull { uriString -> runCatching { Uri.parse(uriString) }.getOrNull() }
        .toSet()
}

fun isSupportedAudioMimeType(mimeType: String?): Boolean {
    if (mimeType.isNullOrBlank()) return false
    val normalized = mimeType.lowercase()
    return normalized.startsWith("audio/") ||
        normalized.endsWith(".mp3") ||
        normalized.endsWith(".wav") ||
        normalized.endsWith(".m4a") ||
        normalized.endsWith(".aac") ||
        normalized.endsWith(".ogg")
}

fun sanitizeFileName(fileName: String): String {
    val trimmed = fileName.trim()
    val safe = trimmed.replace(Regex("[^A-Za-z0-9._-]"), "_")
    return if (safe.isBlank()) "audio_file" else safe
}

fun isSupportedAudioUri(contentResolver: ContentResolver, uri: Uri): Boolean {
    val mimeType = contentResolver.getType(uri)
    return isSupportedAudioMimeType(mimeType)
}

fun copyUriToAppStorage(context: Context, uri: Uri): Uri? {
    return try {
        val originalName = displayName(context.contentResolver, uri)
        val safeFileName = sanitizeFileName(originalName)
        val destination = File(context.filesDir, safeFileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        Uri.fromFile(destination)
    } catch (_: Exception) {
        null
    }
}

fun loadTrackMetadata(context: Context, uri: Uri): TrackMetadata {
    return try {
        val retriever = MediaMetadataRetriever()
        try {
            when (uri.scheme) {
                "content", "android.resource" -> retriever.setDataSource(context, uri)
                "file" -> retriever.setDataSource(uri.path)
                else -> {
                    val savedUri = copyUriToAppStorage(context, uri)
                    if (savedUri == null) return TrackMetadata(
                        uri = uri,
                        title = displayName(context.contentResolver, uri),
                        artist = DEFAULT_ARTIST,
                        artwork = null
                    )
                    retriever.setDataSource(context, savedUri)
                }
            }

            val embedded = retriever.embeddedPicture
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: displayName(context.contentResolver, uri)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: DEFAULT_ARTIST
            val artwork = embedded?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            TrackMetadata(uri, title, artist, artwork)
        } finally {
            retriever.release()
        }
    } catch (_: Exception) {
        TrackMetadata(
            uri = uri,
            title = displayName(context.contentResolver, uri),
            artist = DEFAULT_ARTIST,
            artwork = loadAlbumArt(context, uri)
        )
    }
}

fun loadAlbumArt(context: Context, uri: Uri): Bitmap? {
    return try {
        val retriever = MediaMetadataRetriever()
        try {
            when (uri.scheme) {
                "content", "android.resource" -> retriever.setDataSource(context, uri)
                "file" -> retriever.setDataSource(uri.path)
                else -> {
                    val savedUri = copyUriToAppStorage(context, uri)
                    if (savedUri == null) return null
                    retriever.setDataSource(context, savedUri)
                }
            }

            val embedded = retriever.embeddedPicture ?: return null
            BitmapFactory.decodeByteArray(embedded, 0, embedded.size)
        } finally {
            retriever.release()
        }
    } catch (_: Exception) {
        null
    }
}
