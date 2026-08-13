package com.example.xargoosh.domain.editor

import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import com.example.xargoosh.R

object MetadataEditor {
    private const val MAX_COVER_INPUT_BYTES = 8 * 1024 * 1024
    private const val MAX_COVER_DIMENSION = 1200
    sealed interface EditResult {
        data object Success : EditResult
        data class PermissionRequired(val message: String) : EditResult
        data class Failure(val message: String, val cause: Throwable? = null) : EditResult
    }

    suspend fun editMetadata(
        context: Context,
        uri: Uri,
        title: String?,
        artist: String?,
        album: String?,
        coverArtUri: Uri?,
        extension: String? = null,
        intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    ): EditResult = withContext(Dispatchers.IO) {
        val resolvedExtension = resolveExtension(context, uri, extension)
        val tempFile = runCatching {
            File.createTempFile("metadata_edit_", ".$resolvedExtension", context.cacheDir)
        }.getOrElse {
            return@withContext EditResult.Failure(context.getString(R.string.metadata_temp_failed), it)
        }

        try {
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                true
            } ?: false
            if (!copied) return@withContext EditResult.Failure(context.getString(R.string.metadata_read_failed))

            val audioFile = AudioFileIO.read(tempFile)
            val tag = audioFile.tagOrCreateAndSetDefault
            title?.let { tag.setField(FieldKey.TITLE, it.trim()) }
            artist?.let { tag.setField(FieldKey.ARTIST, it.trim()) }
            album?.let { tag.setField(FieldKey.ALBUM, it.trim()) }

            if (coverArtUri != null) {
                val artBytes = context.contentResolver.openInputStream(coverArtUri)?.use { input ->
                    val raw = readBounded(context, input, MAX_COVER_INPUT_BYTES)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
                        bounds.outWidth > 20_000 || bounds.outHeight > 20_000
                    ) return@withContext EditResult.Failure(context.getString(R.string.metadata_cover_dimensions))
                    var sampleSize = 1
                    while (bounds.outWidth / sampleSize > MAX_COVER_DIMENSION * 2 ||
                        bounds.outHeight / sampleSize > MAX_COVER_DIMENSION * 2
                    ) sampleSize *= 2
                    val bitmap = BitmapFactory.decodeByteArray(
                        raw,
                        0,
                        raw.size,
                        BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    )
                        ?: return@withContext EditResult.Failure(context.getString(R.string.metadata_cover_invalid))
                    val scale = minOf(1f, MAX_COVER_DIMENSION.toFloat() / maxOf(bitmap.width, bitmap.height))
                    val resized = if (scale < 1f) Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt().coerceAtLeast(1),
                        (bitmap.height * scale).toInt().coerceAtLeast(1),
                        true
                    ) else bitmap
                    ByteArrayOutputStream().use { output ->
                        resized.compress(Bitmap.CompressFormat.JPEG, 88, output)
                        if (resized !== bitmap) resized.recycle()
                        bitmap.recycle()
                        output.toByteArray()
                    }
                }
                    ?: return@withContext EditResult.Failure(context.getString(R.string.metadata_cover_read_failed))
                val artwork = ArtworkFactory.getNew().apply {
                    binaryData = artBytes
                    mimeType = "image/jpeg"
                }
                tag.deleteArtworkField()
                tag.setField(artwork)
            }

            audioFile.commit()
            try {
                writeBackToUri(context, uri, tempFile)
                EditResult.Success
            } catch (securityException: SecurityException) {
                requestWriteAccess(context, uri, securityException, intentSenderLauncher)
            }
        } catch (exception: Exception) {
            EditResult.Failure(
                exception.message?.takeIf { it.isNotBlank() } ?: context.getString(R.string.metadata_update_failed),
                exception
            )
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    suspend fun writeBackToUri(context: Context, uri: Uri, sourceFile: File) =
        withContext(Dispatchers.IO) {
            val recoveryDir = File(context.filesDir, "metadata_recovery").apply { mkdirs() }
            val backup = File.createTempFile("metadata_backup_", ".audio", recoveryDir)
            var preserveBackup = false
            var backupComplete = false
            var destinationModified = false
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(backup).use { output ->
                        input.copyTo(output)
                        output.flush()
                        output.fd.sync()
                    }
                } ?: throw SecurityException(context.getString(R.string.metadata_file_unreadable))
                backupComplete = true

                writeFileDescriptor(context, uri, sourceFile) { destinationModified = true }
                val writtenDigest = context.contentResolver.openInputStream(uri)?.use(::sha256)
                    ?: throw java.io.IOException(context.getString(R.string.metadata_reopen_failed))
                if (!writtenDigest.contentEquals(sourceFile.inputStream().use(::sha256))) {
                    throw java.io.IOException(context.getString(R.string.metadata_verify_failed))
                }
            } catch (failure: Throwable) {
                if (!backupComplete || !destinationModified) throw failure
                val restoreFailure = runCatching { writeFileDescriptor(context, uri, backup) }.exceptionOrNull()
                if (restoreFailure != null) {
                    preserveBackup = true
                    failure.addSuppressed(restoreFailure)
                    throw java.io.IOException(
                        context.getString(R.string.metadata_restore_failed, backup.absolutePath),
                        failure
                    )
                }
                throw failure
            } finally {
                if (!preserveBackup) backup.delete()
            }
        }

    private fun writeFileDescriptor(
        context: Context,
        uri: Uri,
        source: File,
        onWriteStarted: () -> Unit = {}
    ) {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "rw")
            ?: throw SecurityException(context.getString(R.string.metadata_file_unwritable))
        descriptor.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).use { output ->
                onWriteStarted()
                output.channel.truncate(0)
                output.channel.position(0)
                FileInputStream(source).use { input -> input.copyTo(output) }
                output.flush()
                pfd.fileDescriptor.sync()
            }
        }
    }

    private fun sha256(input: java.io.InputStream): ByteArray {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        return digest.digest()
    }

    private fun readBounded(context: Context, input: java.io.InputStream, maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) throw java.io.IOException(context.getString(R.string.metadata_cover_too_large))
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private suspend fun requestWriteAccess(
        context: Context,
        uri: Uri,
        exception: SecurityException,
        launcher: ActivityResultLauncher<IntentSenderRequest>?
    ): EditResult {
        if (launcher == null) {
            return EditResult.Failure(context.getString(R.string.metadata_permission_required), exception)
        }

        val request = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val pendingIntent = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            }
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                val recoverable = exception as? RecoverableSecurityException
                    ?: return EditResult.Failure(context.getString(R.string.metadata_permission_denied), exception)
                IntentSenderRequest.Builder(recoverable.userAction.actionIntent.intentSender).build()
            }
            else -> return EditResult.Failure(context.getString(R.string.metadata_permission_denied), exception)
        }

        withContext(Dispatchers.Main) { launcher.launch(request) }
        return EditResult.PermissionRequired(context.getString(R.string.metadata_grant_access))
    }

    private fun resolveExtension(context: Context, uri: Uri, fallback: String?): String {
        val displayName = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        val fromName = displayName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
        val fromMime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
            ?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
        return (fromName ?: fromMime ?: fallback ?: "mp3")
            .lowercase()
            .filter { it.isLetterOrDigit() }
            .ifBlank { "mp3" }
    }
}
