package com.example.xargoosh.utils

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import java.io.File
import com.example.xargoosh.R

object TrackUtils {

    fun shareTracks(context: Context, trackUris: Collection<String>) {
        val uris = ArrayList(trackUris.distinct().map(Uri::parse))
        if (uris.isEmpty()) return
        val sendIntent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, context.getString(R.string.shared_music), uris.first()).also { clip ->
                uris.drop(1).forEach { clip.addItem(android.content.ClipData.Item(it)) }
            }
            if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first())
            else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
        context.startActivity(Intent.createChooser(sendIntent, context.getString(if (uris.size == 1) R.string.share_song else R.string.share_songs)))
    }

    fun setAsRingtone(context: Context, trackUri: String) {
        val uri = Uri.parse(trackUri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                Toast.makeText(context, context.getString(R.string.permission_modify_settings), Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }
        try {
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE,
                uri
            )
            Toast.makeText(context, context.getString(R.string.ringtone_set_successfully), Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.ringtone_set_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteTrack(
        context: Context,
        trackId: String,
        onIntentSenderRequired: (IntentSenderRequest) -> Unit,
        onSuccess: () -> Unit,
        onFailure: () -> Unit = {}
    ) {
        val uri = if (trackId.startsWith("content://")) {
            Uri.parse(trackId)
        } else {
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId.toLongOrNull() ?: return)
        }
        try {
            val deleted = context.contentResolver.delete(uri, null, null)
            if (deleted > 0) {
                Toast.makeText(context, context.getString(R.string.deleted_from_device), Toast.LENGTH_SHORT).show()
                onSuccess()
            } else onFailure()
        } catch (securityException: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching { MediaStore.createDeleteRequest(context.contentResolver, listOf(uri)).intentSender }
                    .onSuccess { onIntentSenderRequired(IntentSenderRequest.Builder(it).build()) }
                    .onFailure { onFailure() }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val recoverableSecurityException = securityException as? android.app.RecoverableSecurityException
                val sender = recoverableSecurityException?.userAction?.actionIntent?.intentSender
                if (sender != null) {
                    onIntentSenderRequired(IntentSenderRequest.Builder(sender).build())
                } else onFailure()
            } else {
                Toast.makeText(context, context.getString(R.string.delete_permission_denied), Toast.LENGTH_SHORT).show()
                onFailure()
            }
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.delete_failed), Toast.LENGTH_SHORT).show()
            onFailure()
        }
    }
}
