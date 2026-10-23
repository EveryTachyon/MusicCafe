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
import java.security.MessageDigest

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
private const val PLAYLISTS_KEY = "playlists"

fun persistReadPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
    }
}

fun saveImportedSongs(context: Context, songs: List<Uri>) {
    val prefs = context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
    val encoded = JSONArray().apply { songs.distinct().forEach { put(it.toString()) } }.toString()
    prefs.edit().putString(IMPORTED_SONGS_KEY, encoded).apply()
}

fun loadImportedSongs(context: Context): List<Uri> {
    val prefs = context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
    val encoded = runCatching { prefs.getString(IMPORTED_SONGS_KEY, null) }.getOrNull()
    val uriStrings = if (encoded != null) {
        runCatching {
            val array = JSONArray(encoded)
            (0 until array.length()).map { index -> array.optString(index) }
        }.getOrDefault(emptyList())
    } else {
        // Migrate the old unordered StringSet format for existing libraries.
        prefs.getStringSet(IMPORTED_SONGS_KEY, emptySet())?.toList() ?: emptyList()
    }
    return uriStrings.mapNotNull { uriString ->
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
    val encoded = JSONArray().apply {
        playlists.forEach { playlist ->
            put(JSONObject().apply {
                put("name", playlist.name)
                put("songs", JSONArray().apply { playlist.songs.forEach { put(it.toString()) } })
            })
        }
    }.toString()
    context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
        .edit().putString(PLAYLISTS_KEY, encoded).apply()
}

fun loadPlaylists(context: Context): List<Playlist> {
    val preferences = context.getSharedPreferences(APP_STORAGE_PREFS, Context.MODE_PRIVATE)
    val encoded = runCatching { preferences.getString(PLAYLISTS_KEY, null) }.getOrNull()
    if (encoded != null) {
        return runCatching {
            JSONArray(encoded).let { array ->
                (0 until array.length()).mapNotNull { index ->
                    val item = array.optJSONObject(index) ?: return@mapNotNull null
                    val name = item.optString("name").trim()
                    if (name.isBlank()) return@mapNotNull null
                    val songArray = item.optJSONArray("songs")
                    val songs = if (songArray == null) emptySet() else {
                        (0 until songArray.length()).mapNotNull { songIndex ->
                            songArray.optString(songIndex).takeIf { it.isNotBlank() }?.let(Uri::parse)
                        }.toSet()
                    }
                    Playlist(name, songs)
                }
            }
        }.getOrDefault(emptyList())
    }

    // Read the old delimiter format so existing libraries survive the update.
    val legacyValues = preferences.getStringSet(PLAYLISTS_KEY, emptySet()) ?: emptySet()
    return legacyValues.mapNotNull { encodedPlaylist ->
        val separator = encodedPlaylist.indexOf('|')
        if (separator <= 0) return@mapNotNull null
        val name = encodedPlaylist.substring(0, separator).trim()
        val songs = encodedPlaylist.substring(separator + 1).split(',').mapNotNull { value ->
            value.takeIf { it.isNotBlank() }?.let(Uri::parse)
        }.toSet()
        Playlist(name, songs)
    }
}

fun isSupportedAudioMimeType(mimeType: String?): Boolean {
    if (mimeType.isNullOrBlank()) return false
    return mimeType.lowercase().startsWith("audio/")
}

private val supportedAudioExtensions = setOf("mp3", "wav", "m4a", "aac", "ogg", "flac", "opus", "wma", "amr", "3gp")

fun sanitizeFileName(fileName: String): String {
    val trimmed = fileName.trim()
    if (trimmed.isBlank()) return "audio_file"

    val safe = trimmed.replace(Regex("[^A-Za-z0-9._-]"), "_")
    val separatorIndex = safe.lastIndexOf('.')
    val baseName = if (separatorIndex > 0 && separatorIndex < safe.length - 1) {
        safe.substring(0, separatorIndex).trim('_') + safe.substring(separatorIndex)
    } else {
        safe.trim('_')
    }

    return if (baseName.isBlank()) "audio_file" else baseName
}

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead < 0) break
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}

fun isSupportedAudioUri(contentResolver: ContentResolver, uri: Uri): Boolean {
    if (isSupportedAudioMimeType(contentResolver.getType(uri))) return true
    val extension = displayName(contentResolver, uri).substringAfterLast('.', "").lowercase()
    return extension in supportedAudioExtensions
}

fun copyUriToAppStorage(context: Context, uri: Uri): Uri? {
    var temporaryFile: File? = null
    return try {
        val originalName = displayName(context.contentResolver, uri)
        val safeFileName = sanitizeFileName(originalName)
        val extension = safeFileName.substringAfterLast('.', "")
        val baseName = if (extension.isBlank()) safeFileName else safeFileName.removeSuffix(".$extension")
        temporaryFile = File.createTempFile("musiccafe-import-", ".tmp", context.filesDir)
        val digest = MessageDigest.getInstance("SHA-256")
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { source ->
            temporaryFile!!.outputStream().buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val bytesRead = source.read(buffer)
                    if (bytesRead < 0) break
                    digest.update(buffer, 0, bytesRead)
                    output.write(buffer, 0, bytesRead)
                }
            }
        }

        val contentHash = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        val existingFile = context.filesDir.listFiles()?.firstOrNull { file ->
            file.isFile && file != temporaryFile && file.length() == temporaryFile!!.length() &&
                sha256(file) == contentHash
        }
        if (existingFile != null) return Uri.fromFile(existingFile)

        var destination = File(context.filesDir, safeFileName)
        var suffix = 1
        while (destination.exists()) {
            destination = File(
                context.filesDir,
                if (extension.isBlank()) "$baseName-$suffix" else "$baseName-$suffix.$extension"
            )
            suffix++
        }
        check(temporaryFile!!.renameTo(destination))
        Uri.fromFile(destination)
    } catch (_: Exception) {
        null
    } finally {
        temporaryFile?.delete()
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
