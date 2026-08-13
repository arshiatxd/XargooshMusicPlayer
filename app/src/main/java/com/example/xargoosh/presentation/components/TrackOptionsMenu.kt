package com.example.xargoosh.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.xargoosh.domain.models.MusicTrack
import com.example.xargoosh.core.design.themes.XargooshTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.xargoosh.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsMenu(
    track: MusicTrack,
    onDismiss: () -> Unit,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onEditTags: (() -> Unit)? = null,
    onGoToAlbum: (() -> Unit)? = null,
    onGoToArtist: (() -> Unit)? = null,
    onChangeCover: (() -> Unit)? = null,
    onSetAsRingtone: (() -> Unit)? = null,
    onDeleteFromDevice: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val runAndDismiss: (() -> Unit) -> Unit = { action ->
        onDismiss()
        action()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = XargooshTheme.colors.surface.copy(alpha = 0.99f),
        contentColor = XargooshTheme.colors.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.74f),
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 18.dp)
            ) {
                AudioThumbnail(
                    uri = track.uri,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title.ifBlank { stringResource(R.string.unknown_title) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = track.artist.ifBlank { stringResource(R.string.unknown_artist) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            Spacer(Modifier.height(8.dp))
            onPlayNext?.let { action -> MenuItem(Icons.Outlined.ArrowForward, stringResource(R.string.play_next)) { runAndDismiss(action) } }
            onAddToQueue?.let { action -> MenuItem(Icons.Outlined.PlaylistAdd, stringResource(R.string.add_to_queue)) { runAndDismiss(action) } }
            onAddToPlaylist?.let { action -> MenuItem(Icons.Outlined.LibraryAdd, stringResource(R.string.add_to_playlist_lower)) { runAndDismiss(action) } }
            MenuItem(Icons.Outlined.Share, stringResource(R.string.share)) { runAndDismiss { com.example.xargoosh.utils.TrackUtils.shareTracks(context, listOf(track.uri)) } }
            onGoToAlbum?.let { action -> MenuItem(Icons.Outlined.Album, stringResource(R.string.go_to_album), track.album) { runAndDismiss(action) } }
            onGoToArtist?.let { action -> MenuItem(Icons.Outlined.Person, stringResource(R.string.go_to_artist), track.artist) { runAndDismiss(action) } }
            onEditTags?.let { action -> MenuItem(Icons.Outlined.Edit, stringResource(R.string.edit_song_info)) { runAndDismiss(action) } }
            onChangeCover?.let { action -> MenuItem(Icons.Outlined.Image, stringResource(R.string.change_cover)) { runAndDismiss(action) } }
            onSetAsRingtone?.let { action -> MenuItem(Icons.Outlined.Alarm, stringResource(R.string.set_as_ringtone)) { runAndDismiss(action) } }

            Spacer(Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            Spacer(Modifier.height(8.dp))
            onDeleteFromDevice?.let { action ->
                MenuItem(
                    icon = Icons.Outlined.DeleteOutline,
                    label = stringResource(R.string.delete_from_device),
                    tint = MaterialTheme.colorScheme.error
                ) { runAndDismiss(action) }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.close), fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    supportingText: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = tint),
        contentPadding = ButtonDefaults.TextButtonContentPadding
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(23.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            if (!supportingText.isNullOrBlank()) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = tint.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
