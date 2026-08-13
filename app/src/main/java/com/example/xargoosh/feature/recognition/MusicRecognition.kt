package com.example.xargoosh.feature.recognition

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.StringReader
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

const val RECOGNITION_SAMPLE_RATE_HZ = 16_000
const val RECOGNITION_MAX_CAPTURE_SECONDS = 12

data class RecognizedMusic(
    val artist: String,
    val title: String,
    val album: String?,
    val releaseDate: String?,
    val songLink: String?,
    val artworkUrl: String?,
    val appleMusicUrl: String?,
    val spotifyUrl: String?,
    val deezerUrl: String?,
)

data class AudDApiError(
    val code: Int?,
    val message: String,
)

sealed interface AudDResponse {
    data class Match(val music: RecognizedMusic) : AudDResponse
    data object NoMatch : AudDResponse
    data class ApiError(val error: AudDApiError) : AudDResponse
    data class Malformed(val reason: String) : AudDResponse
}

sealed interface RecognitionResult {
    data class Match(val music: RecognizedMusic) : RecognitionResult
    data object NoMatch : RecognitionResult
    data class Failure(val error: RecognitionError) : RecognitionResult
}

sealed interface RecognitionError {
    data object PermissionDenied : RecognitionError
    data object AppNotInForeground : RecognitionError
    data class AudioInitialization(
        val failure: AudioInitializationFailure,
        val platformCode: Int? = null,
        val detail: String? = null,
    ) : RecognitionError

    data class AudioRead(
        val failure: AudioReadFailure,
        val platformCode: Int? = null,
        val detail: String? = null,
    ) : RecognitionError

    data object EmptyCapture : RecognitionError
    data class Network(val failure: NetworkFailure, val detail: String? = null) : RecognitionError
    data class HttpStatus(val statusCode: Int) : RecognitionError
    data object ResponseTooLarge : RecognitionError
    data class AudD(val error: AudDApiError) : RecognitionError
    data class InvalidResponse(val reason: String) : RecognitionError
}

enum class AudioInitializationFailure {
    MINIMUM_BUFFER_QUERY_FAILED,
    RECORDER_CONSTRUCTION_FAILED,
    RECORDER_NOT_INITIALIZED,
    UNEXPECTED_AUDIO_FORMAT,
    RECORDING_START_FAILED,
}

enum class AudioReadFailure {
    BAD_VALUE,
    INVALID_OPERATION,
    DEAD_OBJECT,
    UNKNOWN,
    EXCEPTION,
}

enum class NetworkFailure {
    TIMEOUT,
    IO,
}

class MusicRecognitionRepository(
    context: Context,
    apiToken: String,
    private val isAppInForeground: () -> Boolean,
) {
    private val appContext = context.applicationContext
    private val apiToken = apiToken.trim().also {
        require(it.isNotEmpty()) { "AudD API token must not be blank." }
        require('\r' !in it && '\n' !in it) { "AudD API token must not contain line breaks." }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun recognize(onCaptureFinished: () -> Unit = {}): RecognitionResult = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        if (!hasRecordAudioPermission()) {
            return@withContext RecognitionResult.Failure(RecognitionError.PermissionDenied)
        }

        val capture = captureWav()
        if (capture is CaptureResult.Failure) {
            return@withContext RecognitionResult.Failure(capture.error)
        }
        capture as CaptureResult.Success

        currentCoroutineContext().ensureActive()
        try {
            onCaptureFinished()
        } catch (error: CancellationException) {
            throw error
        } catch (_: RuntimeException) {
        }
        val response = try {
            uploadWav(capture.wav)
        } catch (error: CancellationException) {
            throw error
        } catch (_: ResponseTooLargeException) {
            return@withContext RecognitionResult.Failure(RecognitionError.ResponseTooLarge)
        } catch (error: SocketTimeoutException) {
            return@withContext RecognitionResult.Failure(
                RecognitionError.Network(NetworkFailure.TIMEOUT, error.message),
            )
        } catch (error: IOException) {
            return@withContext RecognitionResult.Failure(
                RecognitionError.Network(NetworkFailure.IO, error.message),
            )
        }

        val parsed = response.body.takeIf(String::isNotBlank)?.let(::parseAudDResponse)
        if (response.statusCode !in 200..299) {
            val apiError = (parsed as? AudDResponse.ApiError)?.error
            return@withContext RecognitionResult.Failure(
                apiError?.let(RecognitionError::AudD)
                    ?: RecognitionError.HttpStatus(response.statusCode),
            )
        }

        when (parsed) {
            is AudDResponse.Match -> RecognitionResult.Match(parsed.music)
            AudDResponse.NoMatch -> RecognitionResult.NoMatch
            is AudDResponse.ApiError -> RecognitionResult.Failure(RecognitionError.AudD(parsed.error))
            is AudDResponse.Malformed -> RecognitionResult.Failure(
                RecognitionError.InvalidResponse(parsed.reason),
            )
            null -> RecognitionResult.Failure(RecognitionError.InvalidResponse("AudD response was empty."))
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private suspend fun captureWav(): CaptureResult {
        if (!foregroundNow()) {
            return CaptureResult.Failure(RecognitionError.AppNotInForeground)
        }

        val minimumBufferBytes = try {
            AudioRecord.getMinBufferSize(
                RECOGNITION_SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
        } catch (error: RuntimeException) {
            return CaptureResult.Failure(
                RecognitionError.AudioInitialization(
                    AudioInitializationFailure.MINIMUM_BUFFER_QUERY_FAILED,
                    detail = error.message,
                ),
            )
        }
        if (minimumBufferBytes <= 0) {
            return CaptureResult.Failure(
                RecognitionError.AudioInitialization(
                    AudioInitializationFailure.MINIMUM_BUFFER_QUERY_FAILED,
                    platformCode = minimumBufferBytes,
                ),
            )
        }

        val requestedBufferBytes = max(minimumBufferBytes, READ_CHUNK_SAMPLES * PCM_BYTES_PER_SAMPLE)
            .let { if (it % PCM_BYTES_PER_SAMPLE == 0) it else it + 1 }
        var recorder: AudioRecord? = null
        var recordingStarted = false
        var capturedSamples: ShortArray? = null
        var capturedSampleCount = 0

        try {
            currentCoroutineContext().ensureActive()
            if (!hasRecordAudioPermission()) {
                return CaptureResult.Failure(RecognitionError.PermissionDenied)
            }
            if (!foregroundNow()) {
                return CaptureResult.Failure(RecognitionError.AppNotInForeground)
            }

            recorder = try {
                createAudioRecord(MediaRecorder.AudioSource.UNPROCESSED, requestedBufferBytes)
            } catch (error: SecurityException) {
                return CaptureResult.Failure(RecognitionError.PermissionDenied)
            } catch (error: RuntimeException) {
                try {
                    createAudioRecord(MediaRecorder.AudioSource.MIC, requestedBufferBytes)
                } catch (fallbackError: SecurityException) {
                    return CaptureResult.Failure(RecognitionError.PermissionDenied)
                } catch (fallbackError: RuntimeException) {
                    return CaptureResult.Failure(
                        RecognitionError.AudioInitialization(
                            AudioInitializationFailure.RECORDER_CONSTRUCTION_FAILED,
                            detail = fallbackError.message ?: error.message,
                        ),
                    )
                }
            }

            val audioRecord = recorder
            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                return CaptureResult.Failure(
                    RecognitionError.AudioInitialization(
                        AudioInitializationFailure.RECORDER_NOT_INITIALIZED,
                    ),
                )
            }
            if (
                audioRecord.sampleRate != RECOGNITION_SAMPLE_RATE_HZ ||
                audioRecord.channelCount != 1 ||
                audioRecord.audioFormat != AudioFormat.ENCODING_PCM_16BIT
            ) {
                return CaptureResult.Failure(
                    RecognitionError.AudioInitialization(
                        AudioInitializationFailure.UNEXPECTED_AUDIO_FORMAT,
                    ),
                )
            }
            if (!hasRecordAudioPermission()) {
                return CaptureResult.Failure(RecognitionError.PermissionDenied)
            }
            if (!foregroundNow()) {
                return CaptureResult.Failure(RecognitionError.AppNotInForeground)
            }

            try {
                audioRecord.startRecording()
            } catch (error: SecurityException) {
                return CaptureResult.Failure(RecognitionError.PermissionDenied)
            } catch (error: RuntimeException) {
                return CaptureResult.Failure(
                    RecognitionError.AudioInitialization(
                        AudioInitializationFailure.RECORDING_START_FAILED,
                        detail = error.message,
                    ),
                )
            }
            recordingStarted = audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING
            if (!recordingStarted) {
                return CaptureResult.Failure(
                    RecognitionError.AudioInitialization(
                        AudioInitializationFailure.RECORDING_START_FAILED,
                    ),
                )
            }

            val samples = ShortArray(MAX_CAPTURE_SAMPLES)
            var sampleCount = 0
            val startedAtNanos = System.nanoTime()
            while (
                sampleCount < samples.size &&
                System.nanoTime() - startedAtNanos < MAX_CAPTURE_NANOS
            ) {
                currentCoroutineContext().ensureActive()
                if (!foregroundNow()) {
                    return CaptureResult.Failure(RecognitionError.AppNotInForeground)
                }

                val readCount = try {
                    audioRecord.read(
                        samples,
                        sampleCount,
                        min(READ_CHUNK_SAMPLES, samples.size - sampleCount),
                        AudioRecord.READ_NON_BLOCKING,
                    )
                } catch (error: SecurityException) {
                    return CaptureResult.Failure(RecognitionError.PermissionDenied)
                } catch (error: RuntimeException) {
                    return CaptureResult.Failure(
                        RecognitionError.AudioRead(
                            AudioReadFailure.EXCEPTION,
                            detail = error.message,
                        ),
                    )
                }

                when {
                    readCount > 0 -> sampleCount += readCount
                    readCount == 0 -> delay(READ_POLL_DELAY_MILLIS)
                    else -> return CaptureResult.Failure(readError(readCount))
                }
            }

            currentCoroutineContext().ensureActive()
            if (sampleCount == 0) {
                return CaptureResult.Failure(RecognitionError.EmptyCapture)
            }
            capturedSamples = samples
            capturedSampleCount = sampleCount
        } catch (error: CancellationException) {
            throw error
        } catch (_: SecurityException) {
            return CaptureResult.Failure(RecognitionError.PermissionDenied)
        } catch (error: RuntimeException) {
            val recognitionError = if (recordingStarted) {
                RecognitionError.AudioRead(AudioReadFailure.EXCEPTION, detail = error.message)
            } else {
                RecognitionError.AudioInitialization(
                    AudioInitializationFailure.RECORDER_CONSTRUCTION_FAILED,
                    detail = error.message,
                )
            }
            return CaptureResult.Failure(recognitionError)
        } finally {
            recorder?.let { audioRecord ->
                try {
                    audioRecord.stop()
                } catch (_: RuntimeException) {
                } finally {
                    try {
                        audioRecord.release()
                    } catch (_: RuntimeException) {
                    }
                }
            }
        }

        currentCoroutineContext().ensureActive()
        return CaptureResult.Success(
            encodePcm16MonoWav(
                samples = checkNotNull(capturedSamples),
                sampleCount = capturedSampleCount,
                sampleRateHz = RECOGNITION_SAMPLE_RATE_HZ,
            ),
        )
    }

    private fun hasRecordAudioPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun createAudioRecord(audioSource: Int, bufferSizeBytes: Int): AudioRecord =
        AudioRecord.Builder()
            .setAudioSource(audioSource)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RECOGNITION_SAMPLE_RATE_HZ)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSizeBytes)
            .build()

    private fun foregroundNow(): Boolean = try {
        isAppInForeground()
    } catch (_: RuntimeException) {
        false
    }

    private suspend fun uploadWav(wav: ByteArray): UploadResponse {
        currentCoroutineContext().ensureActive()
        val boundary = "Xargoosh-${UUID.randomUUID()}"
        val preamble = multipartPreamble(boundary, apiToken)
        val epilogue = "\r\n--$boundary--\r\n".toByteArray(StandardCharsets.US_ASCII)
        val contentLength = preamble.size.toLong() + wav.size + epilogue.size
        val connection = URL(AUDD_ENDPOINT).openConnection() as HttpsURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.useCaches = false
        connection.instanceFollowRedirects = false
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = READ_TIMEOUT_MILLIS
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.setRequestProperty("User-Agent", "XargooshMusicPlayer/1.0")
        connection.setFixedLengthStreamingMode(contentLength)

        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { connection.disconnect() }
            if (!continuation.isActive) {
                connection.disconnect()
                return@suspendCancellableCoroutine
            }

            try {
                val response = performUpload(connection, preamble, wav, epilogue) {
                    continuation.isActive
                }
                continuation.resume(response)
            } catch (error: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun performUpload(
        connection: HttpsURLConnection,
        preamble: ByteArray,
        wav: ByteArray,
        epilogue: ByteArray,
        isActive: () -> Boolean,
    ): UploadResponse {
        connection.outputStream.use { output ->
            output.writeCancellable(preamble, isActive)
            output.writeCancellable(wav, isActive)
            output.writeCancellable(epilogue, isActive)
            ensureUploadActive(isActive)
            output.flush()
        }

        ensureUploadActive(isActive)
        val statusCode = connection.responseCode
        val contentLength = connection.contentLengthLong
        if (contentLength > MAX_RESPONSE_BYTES) {
            throw ResponseTooLargeException()
        }
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.use { readBoundedUtf8(it, MAX_RESPONSE_BYTES, isActive) }.orEmpty()
        return UploadResponse(statusCode, body)
    }

    private sealed interface CaptureResult {
        data class Success(val wav: ByteArray) : CaptureResult
        data class Failure(val error: RecognitionError) : CaptureResult
    }

    private data class UploadResponse(val statusCode: Int, val body: String)

    private companion object {
        const val AUDD_ENDPOINT = "https://api.audd.io/"
        const val PCM_BYTES_PER_SAMPLE = 2
        const val READ_CHUNK_SAMPLES = 2_048
        const val READ_POLL_DELAY_MILLIS = 5L
        const val MAX_CAPTURE_SAMPLES = RECOGNITION_SAMPLE_RATE_HZ * RECOGNITION_MAX_CAPTURE_SECONDS
        const val MAX_CAPTURE_NANOS = RECOGNITION_MAX_CAPTURE_SECONDS * 1_000_000_000L
        const val MAX_RESPONSE_BYTES = 1024 * 1024
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 15_000
    }
}

fun encodePcm16MonoWav(
    samples: ShortArray,
    sampleCount: Int = samples.size,
    sampleRateHz: Int = RECOGNITION_SAMPLE_RATE_HZ,
): ByteArray {
    require(sampleCount in 0..samples.size) { "sampleCount must be within the samples array." }
    require(sampleCount <= (Int.MAX_VALUE - WAV_HEADER_BYTES) / PCM_16_BYTES_PER_SAMPLE) {
        "PCM data is too large for an in-memory WAV."
    }
    require(sampleRateHz in 1..Int.MAX_VALUE / PCM_16_BYTES_PER_SAMPLE) {
        "sampleRateHz is out of range."
    }

    val dataBytes = sampleCount * PCM_16_BYTES_PER_SAMPLE
    val buffer = ByteBuffer.allocate(WAV_HEADER_BYTES + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
    buffer.put("RIFF".toByteArray(StandardCharsets.US_ASCII))
    buffer.putInt(WAV_HEADER_BYTES - 8 + dataBytes)
    buffer.put("WAVE".toByteArray(StandardCharsets.US_ASCII))
    buffer.put("fmt ".toByteArray(StandardCharsets.US_ASCII))
    buffer.putInt(16)
    buffer.putShort(1.toShort())
    buffer.putShort(1.toShort())
    buffer.putInt(sampleRateHz)
    buffer.putInt(sampleRateHz * PCM_16_BYTES_PER_SAMPLE)
    buffer.putShort(PCM_16_BYTES_PER_SAMPLE.toShort())
    buffer.putShort(16.toShort())
    buffer.put("data".toByteArray(StandardCharsets.US_ASCII))
    buffer.putInt(dataBytes)
    for (index in 0 until sampleCount) {
        buffer.putShort(samples[index])
    }
    return buffer.array()
}

fun parseAudDResponse(json: String): AudDResponse {
    if (json.isBlank()) return AudDResponse.Malformed("AudD response was empty.")

    val element = try {
        JsonReader(StringReader(json)).use { reader ->
            reader.setLenient(true)
            JsonParser.parseReader(reader)
        }
    } catch (error: Exception) {
        return AudDResponse.Malformed(error.message ?: "AudD response was not valid JSON.")
    }
    if (!element.isJsonObject) {
        return AudDResponse.Malformed("AudD response root was not an object.")
    }

    val root = element.asJsonObject
    val status = root.stringOrNull("status")
    val apiError = root.objectOrNull("error")
    if (status.equals("error", ignoreCase = true) || apiError != null) {
        if (apiError == null) {
            return AudDResponse.Malformed("AudD error response did not contain an error object.")
        }
        return AudDResponse.ApiError(
            AudDApiError(
                code = apiError.intOrNull("error_code"),
                message = apiError.stringOrNull("error_message")
                    ?: apiError.stringOrNull("message")
                    ?: "Unknown AudD API error.",
            ),
        )
    }
    if (!status.equals("success", ignoreCase = true)) {
        return AudDResponse.Malformed("AudD response had an unknown status.")
    }

    val resultElement = root.get("result")
        ?: return AudDResponse.Malformed("AudD response did not contain result.")
    if (resultElement.isJsonNull) return AudDResponse.NoMatch
    if (!resultElement.isJsonObject) {
        return AudDResponse.Malformed("AudD result was not an object.")
    }

    val result = resultElement.asJsonObject
    val artist = result.stringOrNull("artist")
        ?: return AudDResponse.Malformed("AudD result did not contain artist.")
    val title = result.stringOrNull("title")
        ?: return AudDResponse.Malformed("AudD result did not contain title.")
    val appleMusic = result.objectOrNull("apple_music")
    val spotify = result.objectOrNull("spotify")
    val deezer = result.objectOrNull("deezer")

    return AudDResponse.Match(
        RecognizedMusic(
            artist = artist,
            title = title,
            album = result.stringOrNull("album"),
            releaseDate = result.stringOrNull("release_date"),
            songLink = result.stringOrNull("song_link"),
            artworkUrl = (result.stringOrNull("artwork_url")
                ?: appleMusic?.objectOrNull("artwork")?.stringOrNull("url")
                ?: spotify?.objectOrNull("album")?.arrayOrNull("images")?.firstObjectString("url")
                ?: deezer?.objectOrNull("album")?.stringOrNull("cover_xl")
                ?: deezer?.objectOrNull("album")?.stringOrNull("cover_big"))
                ?.replace("{w}", "600")
                ?.replace("{h}", "600"),
            appleMusicUrl = appleMusic?.stringOrNull("url"),
            spotifyUrl = spotify?.objectOrNull("external_urls")?.stringOrNull("spotify")
                ?: spotify?.stringOrNull("url"),
            deezerUrl = deezer?.stringOrNull("link") ?: deezer?.stringOrNull("url"),
        ),
    )
}

private fun readError(platformCode: Int): RecognitionError.AudioRead {
    val failure = when (platformCode) {
        AudioRecord.ERROR_BAD_VALUE -> AudioReadFailure.BAD_VALUE
        AudioRecord.ERROR_INVALID_OPERATION -> AudioReadFailure.INVALID_OPERATION
        AudioRecord.ERROR_DEAD_OBJECT -> AudioReadFailure.DEAD_OBJECT
        else -> AudioReadFailure.UNKNOWN
    }
    return RecognitionError.AudioRead(failure, platformCode)
}

private fun multipartPreamble(boundary: String, apiToken: String): ByteArray = buildString {
    append("--").append(boundary).append("\r\n")
    append("Content-Disposition: form-data; name=\"api_token\"\r\n\r\n")
    append(apiToken).append("\r\n")
    append("--").append(boundary).append("\r\n")
    append("Content-Disposition: form-data; name=\"return\"\r\n\r\n")
    append("apple_music,spotify,deezer\r\n")
    append("--").append(boundary).append("\r\n")
    append("Content-Disposition: form-data; name=\"file\"; filename=\"recognition.wav\"\r\n")
    append("Content-Type: audio/wav\r\n\r\n")
}.toByteArray(StandardCharsets.UTF_8)

private fun OutputStream.writeCancellable(bytes: ByteArray, isActive: () -> Boolean) {
    var offset = 0
    while (offset < bytes.size) {
        ensureUploadActive(isActive)
        val count = min(NETWORK_BUFFER_BYTES, bytes.size - offset)
        write(bytes, offset, count)
        offset += count
    }
}

private fun readBoundedUtf8(
    input: InputStream,
    maxBytes: Int,
    isActive: () -> Boolean,
): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(NETWORK_BUFFER_BYTES)
    var total = 0
    while (true) {
        ensureUploadActive(isActive)
        val count = input.read(buffer)
        if (count < 0) break
        if (count > maxBytes - total) throw ResponseTooLargeException()
        output.write(buffer, 0, count)
        total += count
    }
    return output.toString(StandardCharsets.UTF_8.name())
}

private fun ensureUploadActive(isActive: () -> Boolean) {
    if (!isActive()) throw CancellationException("Music recognition upload was cancelled.")
}

private fun JsonObject.stringOrNull(name: String): String? {
    val value = get(name) ?: return null
    if (!value.isJsonPrimitive || !value.asJsonPrimitive.isString) return null
    return value.asString.trim().takeIf(String::isNotEmpty)
}

private fun JsonObject.intOrNull(name: String): Int? {
    val value = get(name) ?: return null
    if (!value.isJsonPrimitive) return null
    return try {
        value.asString.toIntOrNull()
    } catch (_: RuntimeException) {
        null
    }
}

private fun JsonObject.objectOrNull(name: String): JsonObject? {
    val value = get(name) ?: return null
    return if (value.isJsonObject) value.asJsonObject else null
}

private fun JsonObject.arrayOrNull(name: String): JsonArray? {
    val value = get(name) ?: return null
    return if (value.isJsonArray) value.asJsonArray else null
}

private fun JsonArray.firstObjectString(name: String): String? {
    for (element in this) {
        if (element.isJsonObject) {
            element.asJsonObject.stringOrNull(name)?.let { return it }
        }
    }
    return null
}

private class ResponseTooLargeException : IOException()

private const val WAV_HEADER_BYTES = 44
private const val PCM_16_BYTES_PER_SAMPLE = 2
private const val NETWORK_BUFFER_BYTES = 8 * 1024
