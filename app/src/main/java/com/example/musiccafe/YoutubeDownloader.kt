package com.example.musiccafe

import android.content.Context
import android.net.Uri
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

private const val CONNECT_TIMEOUT_MS = 15_000
private const val READ_TIMEOUT_MS = 30_000
private val newPipeInitialized = AtomicBoolean(false)

private class AndroidDownloader : Downloader() {
    override fun execute(request: Request): Response {
        val connection = URL(request.url()).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        return try {
            connection.requestMethod = request.httpMethod()
            request.headers().forEach { (name, values) -> connection.setRequestProperty(name, values.joinToString(",")) }
            Response(
                connection.responseCode,
                connection.responseMessage,
                connection.headerFields.filterKeys { it != null },
                (if (connection.responseCode >= 400) connection.errorStream else connection.inputStream)
                    ?.bufferedReader()?.use { it.readText() } ?: "",
                null
            )
        } finally {
            connection.disconnect()
        }
    }
}

data class DownloadedTrack(val uri: Uri, val title: String)

private fun normalizedYoutubeUrl(input: String): String {
    val parsed = Uri.parse(input.trim())
    val host = parsed.host?.lowercase()
    val videoId = when {
        host == "youtu.be" -> parsed.pathSegments.firstOrNull()
        host == "youtube.com" || host?.endsWith(".youtube.com") == true -> {
            parsed.getQueryParameter("v")
                ?: parsed.pathSegments.takeIf { it.size >= 2 && it.first() in setOf("shorts", "embed") }?.get(1)
        }
        else -> null
    }
    require(!videoId.isNullOrBlank() && videoId.matches(Regex("[A-Za-z0-9_-]{6,}"))) {
        "Paste a YouTube video link, for example youtube.com/watch?v=VIDEO_ID"
    }
    return "https://www.youtube.com/watch?v=$videoId"
}

suspend fun downloadYoutubeAudio(context: Context, youtubeUrl: String): DownloadedTrack = withContext(Dispatchers.IO) {
    val normalizedUrl = normalizedYoutubeUrl(youtubeUrl)
    if (newPipeInitialized.compareAndSet(false, true)) {
        NewPipe.init(AndroidDownloader())
    }
    val info = StreamInfo.getInfo(ServiceList.YouTube, normalizedUrl)
    val stream = info.audioStreams.maxByOrNull { it.averageBitrate }
        ?: error("This video has no audio stream")
    val safeTitle = sanitizeFileName(info.name).ifBlank { "youtube_audio" }
    val sourceExtension = stream.format?.suffix?.lowercase()?.takeIf { it.matches(Regex("[a-z0-9]+")) } ?: "m4a"
    val sourceFile = File.createTempFile("musiccafe-youtube-", ".$sourceExtension", context.cacheDir)
    val destination = nextAvailableFile(context.filesDir, safeTitle, "mp3")
    val connection = URL(stream.content).openConnection() as HttpURLConnection
    connection.connectTimeout = CONNECT_TIMEOUT_MS
    connection.readTimeout = READ_TIMEOUT_MS
    try {
        check(connection.responseCode in 200..299) { "Audio download failed (${connection.responseCode})" }
        connection.inputStream.use { input ->
            sourceFile.outputStream().use { output -> input.copyTo(output) }
        }
    } finally {
        connection.disconnect()
    }
    try {
        convertToMp3(context, sourceFile, destination)
    } catch (error: Exception) {
        destination.delete()
        throw error
    } finally {
        sourceFile.delete()
    }
    DownloadedTrack(Uri.fromFile(destination), info.name)
}

private fun nextAvailableFile(directory: File, title: String, extension: String): File {
    var file = File(directory, "$title.$extension")
    var suffix = 1
    while (file.exists()) {
        file = File(directory, "$title-$suffix.$extension")
        suffix++
    }
    return file
}

private fun quoteForShell(path: String): String = "'${path.replace("'", "'\\''")}'"

private suspend fun convertToMp3(context: Context, sourceFile: File, destination: File) =
    kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
        val command = arrayOf(
            "-y",
            "-i", sourceFile.path,
            "-vn",
            "-codec:a", "libmp3lame",
            "-q:a", "2",
            destination.path
        )
        val handler = object : ExecuteBinaryResponseHandler() {
            override fun onSuccess(message: String?) {
                if (continuation.isActive) continuation.resume(Unit) {}
            }

            override fun onFailure(message: String?) {
                if (continuation.isActive) continuation.resumeWith(Result.failure(IllegalStateException(message ?: "Could not convert YouTube audio to MP3")))
            }
        }
        try {
            FFmpeg.getInstance(context).execute(command, handler)
        } catch (error: FFmpegCommandAlreadyRunningException) {
            continuation.resumeWith(Result.failure(error))
        } catch (error: FFmpegNotSupportedException) {
            continuation.resumeWith(Result.failure(error))
        }
    }