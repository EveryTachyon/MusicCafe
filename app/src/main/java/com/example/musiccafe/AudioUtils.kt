package com.example.musiccafe

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.MediaStore
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

data class DeviceAudioFile(
    val uri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val artwork: Bitmap? = null
)

fun queryDeviceAudioFiles(context: Context): List<DeviceAudioFile> {
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION
    )
    val songs = mutableListOf<DeviceAudioFile>()
    context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        "${MediaStore.Audio.Media.IS_MUSIC} != 0",
        null,
        "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIndex)
            songs += DeviceAudioFile(
                uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString()),
                title = cursor.getString(titleIndex).orEmpty().ifBlank { "Unknown song" },
                artist = cursor.getString(artistIndex).orEmpty().ifBlank { "Unknown artist" },
                durationMs = cursor.getLong(durationIndex)
            )
        }
    }
    return songs
}

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
private const val PLAYLISTS_KEY = "playlists"
private const val RECENTLY_PLAYED_KEY = "recently_played"
private const val MAX_RECENTLY_PLAYED = 5

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

fun deleteDownloadedSong(uri: Uri): Boolean {
    if (uri.scheme != "file") return false
    return uri.path?.let { filePath ->
        val file = File(filePath)
        !file.exists() || file.delete()
    } ?: false
}

fun deleteAllDownloadedSongs(songs: Set<Uri>): Set<Uri> {
    return songs.filter { deleteDownloadedSong(it) }.toSet()
}

fun savePlaylists(context: Context, playlists: List<Playlist>) {
    val encoded = playlists.map { playlist ->
        playlist.name.replace("|", "_") + "|" + playlist.songs.joinToString(",") { it.toString() }
    }.toSet()
    context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
        .edit().putStringSet(PLAYLISTS_KEY, encoded).apply()
}

fun loadPlaylists(context: Context): List<Playlist> {
    val values = context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
        .getStringSet(PLAYLISTS_KEY, emptySet()) ?: emptySet()
    return values.mapNotNull { encoded ->
        val separator = encoded.indexOf('|')
        if (separator <= 0) return@mapNotNull null
        val name = encoded.substring(0, separator)
        val songs = encoded.substring(separator + 1).split(',').mapNotNull { value ->
            value.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        }.toSet()
        Playlist(name, songs)
    }
}

fun loadRecentlyPlayed(context: Context): List<Uri> {
    return context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
        .getString(RECENTLY_PLAYED_KEY, "")
        ?.split('\n')
        ?.filter(String::isNotBlank)
        ?.mapNotNull { value -> runCatching { Uri.parse(value) }.getOrNull() }
        ?.take(MAX_RECENTLY_PLAYED)
        ?: emptyList()
}

fun saveRecentlyPlayed(context: Context, songs: List<Uri>) {
    context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(RECENTLY_PLAYED_KEY, songs.take(MAX_RECENTLY_PLAYED).joinToString("\n") { it.toString() })
        .apply()
}

fun addRecentlyPlayed(context: Context, uri: Uri): List<Uri> {
    val updated = listOf(uri) + loadRecentlyPlayed(context).filterNot { it == uri }
    val limited = updated.take(MAX_RECENTLY_PLAYED)
    saveRecentlyPlayed(context, limited)
    return limited
}

fun isSupportedAudioMimeType(mimeType: String?): Boolean {
    if (mimeType.isNullOrBlank()) return false
    val normalized = mimeType.lowercase()
    return normalized.startsWith("audio/")
}

fun sanitizeFileName(fileName: String): String {
    val trimmed = fileName.trim()
    val safe = trimmed.replace(Regex("[^A-Za-z0-9._-]"), "_")
    return if (safe.isBlank()) "audio_file" else safe
}

fun isSupportedAudioUri(contentResolver: ContentResolver, uri: Uri): Boolean {
    val mimeType = contentResolver.getType(uri)
    if (isSupportedAudioMimeType(mimeType)) return true
    if (!mimeType.isNullOrBlank()) return false

    val name = displayName(contentResolver, uri).lowercase()
    return AUDIO_EXTENSIONS.any { extension -> name.endsWith(extension) }
}

private val AUDIO_EXTENSIONS = setOf(".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac", ".opus")

fun copyUriToAppStorage(context: Context, uri: Uri): Uri? {
    return try {
        val originalName = displayName(context.contentResolver, uri)
        val safeFileName = sanitizeFileName(originalName)
        val extension = safeFileName.substringAfterLast('.', "")
        val baseName = if (extension.isBlank()) safeFileName else safeFileName.removeSuffix(".$extension")
        var destination = File(context.filesDir, safeFileName)
        var suffix = 1
        while (destination.exists()) {
            destination = File(
                context.filesDir,
                if (extension.isBlank()) "$baseName-$suffix" else "$baseName-$suffix.$extension"
            )
            suffix++
        }
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

fun loadTrackSummary(context: Context, uri: Uri): TrackMetadata {
    return try {
        val retriever = MediaMetadataRetriever()
        try {
            when (uri.scheme) {
                "content", "android.resource" -> retriever.setDataSource(context, uri)
                "file" -> retriever.setDataSource(uri.path)
                else -> retriever.setDataSource(uri.toString())
            }
            TrackMetadata(
                uri = uri,
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: displayName(context.contentResolver, uri),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: DEFAULT_ARTIST,
                artwork = null
            )
        } finally {
            retriever.release()
        }
    } catch (_: Exception) {
        TrackMetadata(uri, displayName(context.contentResolver, uri), DEFAULT_ARTIST, null)
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
