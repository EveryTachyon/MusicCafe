package com.example.musiccafe

import android.content.Context
import android.net.Uri
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
    val extension = stream.format?.suffix ?: "m4a"
    val baseName = "$safeTitle.$extension"
    var destination = File(context.filesDir, baseName)
    var suffix = 1
    while (destination.exists()) {
        destination = File(context.filesDir, "$safeTitle-$suffix.$extension")
        suffix++
    }
    val connection = URL(stream.content).openConnection() as HttpURLConnection
    connection.connectTimeout = CONNECT_TIMEOUT_MS
    connection.readTimeout = READ_TIMEOUT_MS
    try {
        check(connection.responseCode in 200..299) { "Audio download failed (${connection.responseCode})" }
        connection.inputStream.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
    } finally {
        connection.disconnect()
    }
    DownloadedTrack(Uri.fromFile(destination), info.name)
}