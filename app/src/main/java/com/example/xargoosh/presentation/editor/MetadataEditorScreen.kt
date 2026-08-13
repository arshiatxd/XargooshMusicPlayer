package com.example.xargoosh.presentation.editor

import com.example.xargoosh.core.design.themes.XargooshTheme

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Save
import com.example.xargoosh.core.components.surface.AeroButton as Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.example.xargoosh.core.components.surface.AeroIconButton as IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.xargoosh.domain.editor.MetadataEditor
import com.example.xargoosh.domain.models.MusicTrack
import com.example.xargoosh.presentation.components.AudioThumbnail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.example.xargoosh.R

data class MetadataEditorUiState(
    val track: MusicTrack? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val coverUri: Uri? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    val messageType: MessageType? = null
) {
    enum class MessageType { SUCCESS, ERROR, INFO }
}

class MetadataEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MetadataEditorUiState())
    val uiState: StateFlow<MetadataEditorUiState> = _uiState.asStateFlow()

    fun loadTrack(track: MusicTrack) {
        _uiState.value = MetadataEditorUiState(
            track = track,
            title = track.title,
            artist = track.artist,
            album = track.album
        )
    }

    fun updateTitle(value: String) = _uiState.update { it.copy(title = value, message = null, messageType = null) }
    fun updateArtist(value: String) = _uiState.update { it.copy(artist = value, message = null, messageType = null) }
    fun updateAlbum(value: String) = _uiState.update { it.copy(album = value, message = null, messageType = null) }
    fun updateCover(uri: Uri?) = _uiState.update { it.copy(coverUri = uri, message = null, messageType = null) }

    fun onWritePermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                message = getApplication<Application>().getString(if (granted) R.string.metadata_access_granted else R.string.metadata_access_denied),
                messageType = if (granted) MetadataEditorUiState.MessageType.INFO
                else MetadataEditorUiState.MessageType.ERROR
            )
        }
    }

    fun save(
        context: Context,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onSaved: (MusicTrack) -> Unit
    ) {
        val current = _uiState.value
        val track = current.track ?: run {
            _uiState.update {
                it.copy(message = getApplication<Application>().getString(R.string.metadata_no_track), messageType = MetadataEditorUiState.MessageType.ERROR)
            }
            return
        }
        if (current.title.isBlank()) {
            _uiState.update {
                it.copy(message = getApplication<Application>().getString(R.string.metadata_title_empty), messageType = MetadataEditorUiState.MessageType.ERROR)
            }
            return
        }

        _uiState.update { it.copy(isSaving = true, message = null, messageType = null) }
        viewModelScope.launch {
            when (val result = MetadataEditor.editMetadata(
                context = context.applicationContext,
                uri = Uri.parse(track.uri),
                title = current.title,
                artist = current.artist,
                album = current.album,
                coverArtUri = current.coverUri,
                intentSenderLauncher = launcher
            )) {
                MetadataEditor.EditResult.Success -> {
                    val database = com.example.xargoosh.data.local.db.AppDatabase.getDatabase(getApplication())
                    val updatedTrack = track.copy(title = current.title, artist = current.artist, album = current.album)
                    database.trackDao().updateEditedMetadata(
                        track.uri,
                        updatedTrack.title,
                        updatedTrack.artist,
                        updatedTrack.album
                    )
                    database.lyricsDao().deleteLyrics(track.uri)
                    com.example.xargoosh.presentation.components.invalidateArtwork(track.uri)
                    _uiState.update {
                        it.copy(
                            track = updatedTrack,
                            isSaving = false,
                            message = getApplication<Application>().getString(R.string.metadata_saved),
                            messageType = MetadataEditorUiState.MessageType.SUCCESS
                        )
                    }
                    onSaved(updatedTrack)
                }
                is MetadataEditor.EditResult.PermissionRequired -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = result.message,
                        messageType = MetadataEditorUiState.MessageType.INFO
                    )
                }
                is MetadataEditor.EditResult.Failure -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = result.message,
                        messageType = MetadataEditorUiState.MessageType.ERROR
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataEditorScreen(
    viewModel: MetadataEditorViewModel,
    onNavigateBack: () -> Unit,
    onTrackSaved: (MusicTrack) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.updateCover(uri)
    }
    val writeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onWritePermissionResult(result.resultCode == Activity.RESULT_OK)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.edit_song_info), fontWeight = FontWeight.SemiBold)
                        state.track?.let {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.track == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_track_selected), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(176.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .align(Alignment.CenterHorizontally)
            ) {
                if (state.coverUri != null) {
                    AsyncImage(
                        model = state.coverUri,
                        contentDescription = stringResource(R.string.selected_cover),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AudioThumbnail(
                        uri = state.track?.uri.orEmpty(),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { coverPicker.launch("image/*") }, enabled = !state.isSaving) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(if (state.coverUri == null) R.string.choose_cover else R.string.change_cover))
                }
                if (state.coverUri != null) {
                    IconButton(onClick = { viewModel.updateCover(null) }, enabled = !state.isSaving) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.remove_selected_cover))
                    }
                }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                label = { Text(stringResource(R.string.title)) },
                singleLine = true,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.artist,
                onValueChange = viewModel::updateArtist,
                label = { Text(stringResource(R.string.artist)) },
                singleLine = true,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.album,
                onValueChange = viewModel::updateAlbum,
                label = { Text(stringResource(R.string.album)) },
                singleLine = true,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            state.message?.let { message ->
                val isError = state.messageType == MetadataEditorUiState.MessageType.ERROR
                val isSuccess = state.messageType == MetadataEditorUiState.MessageType.SUCCESS
                val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Card(
                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = color
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(message, style = MaterialTheme.typography.bodyMedium, color = color)
                    }
                }
            }

            if (state.isSaving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { viewModel.save(context, writeLauncher, onTrackSaved) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = if (XargooshTheme.appTheme.isAero) XargooshTheme.colors.onSurface else MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Outlined.Save, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.save_changes), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
