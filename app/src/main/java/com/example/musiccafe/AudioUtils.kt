package com.example.musiccafe

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
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
private const val IMPORTED_SONGS_V2_KEY = "imported_songs_v2"
private const val DOWNLOADED_SONGS_KEY = "downloaded_songs"
private const val DOWNLOADED_SONGS_V2_KEY = "downloaded_songs_v2"
private const val PLAYLISTS_KEY = "playlists"
private const val PLAYLISTS_V2_KEY = "playlists_v2"

fun persistReadPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
    }
}

fun saveImportedSongs(context: Context, songs: List<Uri>) {
    val prefs = context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
    val encoded = JSONArray().apply { songs.distinct().forEach { put(it.toString()) } }.toString()
    prefs.edit().putString(IMPORTED_SONGS_V2_KEY, encoded).apply()
}

fun loadImportedSongs(context: Context): List<Uri> {
    val prefs = context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
    val stored = prefs.getString(IMPORTED_SONGS_V2_KEY, null)?.let(::parseUriArray)
        ?: (prefs.getStringSet(IMPORTED_SONGS_KEY, emptySet()) ?: emptySet()).mapNotNull { value ->
            runCatching { Uri.parse(value) }.getOrNull()
        }
    return stored.map { uri ->
                if (uri.scheme == "content") {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {
                    }
                }
                uri
            }
}

fun saveDownloadedSongs(context: Context, songs: Set<Uri>) {
    val prefs = context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
    val encoded = JSONArray().apply { songs.forEach { put(it.toString()) } }.toString()
    prefs.edit().putString(DOWNLOADED_SONGS_V2_KEY, encoded).apply()
}

fun loadDownloadedSongs(context: Context): Set<Uri> {
    val prefs = context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
    val stored = prefs.getString(DOWNLOADED_SONGS_V2_KEY, null)?.let(::parseUriArray)
        ?: (prefs.getStringSet(DOWNLOADED_SONGS_KEY, emptySet()) ?: emptySet()).mapNotNull { value ->
            runCatching { Uri.parse(value) }.getOrNull()
        }
    return stored
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
    val encoded = JSONArray().apply {
        playlists.forEach { playlist ->
            put(JSONObject().apply {
                put("name", playlist.name)
                put("songs", JSONArray().apply { playlist.songs.forEach { put(it.toString()) } })
            })
        }
    }.toString()
    context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
        .edit().putString(PLAYLISTS_V2_KEY, encoded).apply()
}

fun loadPlaylists(context: Context): List<Playlist> {
    val prefs = context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
    val encoded = prefs.getString(PLAYLISTS_V2_KEY, null)
    if (encoded != null) {
        return runCatching {
            val values = JSONArray(encoded)
            (0 until values.length()).mapNotNull { index ->
                val item = values.optJSONObject(index) ?: return@mapNotNull null
                val name = item.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val songs = item.optJSONArray("songs")?.let { parseUriArray(it.toString()) }?.toSet() ?: emptySet()
                Playlist(name, songs)
            }
        }.getOrDefault(emptyList())
    }

    val legacyValues = prefs.getStringSet(PLAYLISTS_KEY, emptySet()) ?: emptySet()
    return legacyValues.mapNotNull { encodedPlaylist ->
        val separator = encodedPlaylist.indexOf('|')
        if (separator <= 0) return@mapNotNull null
        val name = encodedPlaylist.substring(0, separator)
        val songs = encodedPlaylist.substring(separator + 1).split(',').mapNotNull { value ->
            value.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        }.toSet()
        Playlist(name, songs)
    }
}

private fun parseUriArray(encoded: String): List<Uri> {
    val values = JSONArray(encoded)
    return (0 until values.length()).mapNotNull { index ->
        values.optString(index).takeIf { it.isNotBlank() }?.let { value ->
            runCatching { Uri.parse(value) }.getOrNull()
        }
    }
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
    val withSafeSeparators = trimmed.replace(Regex("[\\s/\\\\]+"), "_")
    val safe = withSafeSeparators.replace(Regex("[^A-Za-z0-9._-]"), "")
    return if (safe.isBlank()) "audio_file" else safe
}

fun isSupportedAudioUri(contentResolver: ContentResolver, uri: Uri): Boolean {
    val mimeType = contentResolver.getType(uri)
    if (isSupportedAudioMimeType(mimeType)) return true
    val fileName = displayName(contentResolver, uri)
    return isSupportedAudioMimeType(fileName)
}

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
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { source ->
            destination.outputStream().use { output -> source.copyTo(output) }
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
