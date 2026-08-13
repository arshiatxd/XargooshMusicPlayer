package com.example.xargoosh.feature.settings.presentation

import com.example.xargoosh.core.design.themes.XargooshTheme
import com.example.xargoosh.core.components.surface.AeroIconButton as IconButton
import com.example.xargoosh.core.components.surface.AeroButton as Button

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.xargoosh.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xargoosh.core.design.themes.AppTheme
import com.example.xargoosh.core.design.themes.ThemeManager
import com.example.xargoosh.feature.player.presentation.PlayerViewModel
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import com.example.xargoosh.domain.playback.AudioPlaybackConfig
import com.example.xargoosh.domain.playback.AudioPlaybackPreferences
import com.example.xargoosh.domain.playback.ReplayGainMode
import com.example.xargoosh.domain.playback.TransitionMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    audioSessionId: Int,
    playerViewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit = {},
    onNavigateToVisualizer: () -> Unit = {}
) {
    val currentTheme by ThemeManager.currentTheme.collectAsStateWithLifecycle()
    val homeViewModel: com.example.xargoosh.feature.library.presentation.HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val context = LocalContext.current
    val visualizerSettings = playerViewModel.visualizerSettings
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showLanguageSelector by remember { mutableStateOf(false) }
    var sleepTimerExpanded by remember { mutableStateOf(false) }
    val leaveSettings = {
        showPrivacyPolicy = false
        showLanguageSelector = false
        sleepTimerExpanded = false
        onNavigateBack()
    }
    val configuration = LocalConfiguration.current
    val selectedLanguageTag = remember(configuration) { currentAppLanguageTag() }
    val selectedLanguageName = if (selectedLanguageTag.isBlank()) stringResource(R.string.system_default) else {
        supportedAppLanguages.firstOrNull { it.tag.equals(selectedLanguageTag, ignoreCase = true) }?.nativeName
            ?: selectedLanguageTag
    }
    val scanStartedMessage = stringResource(R.string.scan_started)
    val audioPreferences = remember(context) { AudioPlaybackPreferences(context) }
    var audioConfig by remember { mutableStateOf(audioPreferences.read()) }
    var pauseOnOtherAudio by remember { mutableStateOf(audioPreferences.pauseOnOtherAudio()) }
    var pauseOnDetach by remember { mutableStateOf(audioPreferences.pauseOnDetach()) }
    DisposableEffect(audioPreferences) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (audioPreferences.isAudioPlaybackKey(key)) audioConfig = audioPreferences.read()
            if (audioPreferences.isPauseOnOtherAudioKey(key)) pauseOnOtherAudio = audioPreferences.pauseOnOtherAudio()
            if (audioPreferences.isPauseOnDetachKey(key)) pauseOnDetach = audioPreferences.pauseOnDetach()
        }
        audioPreferences.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { audioPreferences.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    if (showLanguageSelector) {
        AlertDialog(
            onDismissRequest = { showLanguageSelector = false },
            title = { Text(stringResource(R.string.select_language)) },
            text = {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                    items(supportedAppLanguages.size, key = { supportedAppLanguages[it].tag }) { index ->
                        val language = supportedAppLanguages[index]
                        val selected = language.tag.equals(selectedLanguageTag, ignoreCase = true)
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                showLanguageSelector = false
                                selectAppLanguage(language.tag)
                            }.padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = XargooshTheme.colors.primary))
                            Spacer(Modifier.width(12.dp))
                            Text(if (language.tag.isBlank()) stringResource(R.string.system_default) else language.nativeName, color = XargooshTheme.colors.onSurface, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showLanguageSelector = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            title = { Text(stringResource(R.string.privacy_summary)) },
            text = {
                Text(
                    stringResource(R.string.privacy_summary_text)
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicy = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.settings), color = XargooshTheme.colors.onBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(stringResource(R.string.settings_subtitle), color = XargooshTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = leaveSettings) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = XargooshTheme.colors.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = XargooshTheme.colors.background)
            )
        },
        containerColor = XargooshTheme.colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))



            SettingsCard(title = stringResource(R.string.media), subtitle = stringResource(R.string.media_scanner_settings)) {
                Button(
                    onClick = { 
                        homeViewModel.forceScanLocalMusic() 
                        android.widget.Toast.makeText(context, scanStartedMessage, android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.manual_media_scan))
                }
            }

            ExpandableSettingsCard(title = stringResource(R.string.appearance), subtitle = stringResource(R.string.choose_app_theme), initiallyExpanded = false) {
                val systemDark = isSystemInDarkTheme()
                val lightThemes = AppTheme.entries.filter {
                    it.isLight || (it == AppTheme.DYNAMIC_SYSTEM && !systemDark)
                }
                val darkThemes = AppTheme.entries.filter {
                    (!it.isLight && it != AppTheme.DYNAMIC_SYSTEM) || (it == AppTheme.DYNAMIC_SYSTEM && systemDark)
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.light), color = XargooshTheme.colors.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                lightThemes.forEach { theme ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { ThemeManager.setThemeAndSave(context, theme) }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = theme == currentTheme,
                            onClick = { ThemeManager.setThemeAndSave(context, theme) },
                            colors = RadioButtonDefaults.colors(selectedColor = XargooshTheme.colors.primary)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(theme.displayNameRes),
                            color = if (theme == currentTheme) XargooshTheme.colors.primary else XargooshTheme.colors.onBackground,
                            fontWeight = if (theme == currentTheme) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.dark), color = XargooshTheme.colors.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                darkThemes.forEach { theme ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { ThemeManager.setThemeAndSave(context, theme) }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = theme == currentTheme,
                            onClick = { ThemeManager.setThemeAndSave(context, theme) },
                            colors = RadioButtonDefaults.colors(selectedColor = XargooshTheme.colors.primary)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(theme.displayNameRes),
                            color = if (theme == currentTheme) XargooshTheme.colors.primary else XargooshTheme.colors.onBackground,
                            fontWeight = if (theme == currentTheme) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            SettingsCard(title = stringResource(R.string.language), subtitle = stringResource(R.string.language_subtitle)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showLanguageSelector = true }.padding(top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(selectedLanguageName, color = XargooshTheme.colors.onBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.language_change_hint), color = XargooshTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.select_language), tint = XargooshTheme.colors.onSurfaceVariant)
                }
            }
            
            val useTabbedLayout by visualizerSettings.useTabbedPlayerLayout.collectAsStateWithLifecycle(initialValue = false)
            var useTabbedLayoutUi by remember { mutableStateOf(useTabbedLayout) }
            LaunchedEffect(useTabbedLayout) { useTabbedLayoutUi = useTabbedLayout }
            ExpandableSettingsCard(title = stringResource(R.string.display), subtitle = stringResource(R.string.now_playing_layout), initiallyExpanded = false) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val updated = !useTabbedLayoutUi
                        useTabbedLayoutUi = updated
                        playerViewModel.setTabbedPlayerLayout(updated)
                    }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.tabbed_player_layout), fontSize = 15.sp, color = XargooshTheme.colors.onBackground)
                        Text(stringResource(R.string.tabbed_player_description), fontSize = 12.sp, color = XargooshTheme.colors.onSurfaceVariant)
                    }
                    Switch(
                        checked = useTabbedLayoutUi,
                        onCheckedChange = {
                            useTabbedLayoutUi = it
                            playerViewModel.setTabbedPlayerLayout(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = XargooshTheme.colors.primary, checkedTrackColor = XargooshTheme.colors.primary.copy(alpha = 0.5f))
                    )
                }
            }

            val sleepTimer by playerViewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
            val options = listOf(null, 15, 30, 45, 60, 90, 120)
            val selectedText = sleepTimer?.let { stringResource(R.string.minutes, it) } ?: stringResource(R.string.off)

            SettingsCard(title = stringResource(R.string.sleep_timer), subtitle = stringResource(R.string.sleep_timer_description)) {
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = sleepTimerExpanded,
                    onExpandedChange = { sleepTimerExpanded = !sleepTimerExpanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedText,
                        onValueChange = { },
                        label = { Text(stringResource(R.string.timer)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sleepTimerExpanded) },
                        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = XargooshTheme.colors.primary) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = sleepTimerExpanded,
                        onDismissRequest = { sleepTimerExpanded = false }
                    ) {
                        options.forEach { option ->
                            val text = option?.let { stringResource(R.string.minutes, it) } ?: stringResource(R.string.off)
                            DropdownMenuItem(
                                text = { Text(text) },
                                onClick = {
                                    playerViewModel.setSleepTimer(option)
                                    sleepTimerExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            SettingsCard(title = stringResource(R.string.audio_focus), subtitle = stringResource(R.string.audio_focus_description)) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().toggleable(
                        value = pauseOnOtherAudio,
                        role = Role.Switch,
                        onValueChange = audioPreferences::setPauseOnOtherAudio
                    ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.pause_other_audio), color = XargooshTheme.colors.onBackground, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = pauseOnOtherAudio,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedThumbColor = XargooshTheme.colors.primary, checkedTrackColor = XargooshTheme.colors.primary.copy(alpha = 0.5f))
                    )
                }
            }

            SettingsCard(title = stringResource(R.string.pause_on_detach), subtitle = stringResource(R.string.pause_on_detach_description)) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().toggleable(
                        value = pauseOnDetach,
                        role = Role.Switch,
                        onValueChange = audioPreferences::setPauseOnDetach
                    ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.pause_headphone_detach), color = XargooshTheme.colors.onBackground, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = pauseOnDetach,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedThumbColor = XargooshTheme.colors.primary, checkedTrackColor = XargooshTheme.colors.primary.copy(alpha = 0.5f))
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsCard(title = stringResource(R.string.visualizer), subtitle = stringResource(R.string.visualizer_description)) {
                Spacer(Modifier.height(8.dp))
                SettingsNavigationRow(Icons.Default.Waves, stringResource(R.string.visualizer), stringResource(R.string.visualizer_options_description), onNavigateToVisualizer)
                HorizontalDivider(color = XargooshTheme.colors.outline.copy(alpha = 0.2f))
                SettingsNavigationRow(Icons.Default.GraphicEq, stringResource(R.string.equalizer), stringResource(R.string.equalizer_options_description), onNavigateToEqualizer)
            }

            ExpandableSettingsCard(
                title = stringResource(R.string.audio_transitions),
                subtitle = stringResource(R.string.audio_transitions_description),
                initiallyExpanded = false
            ) {
                Text(stringResource(R.string.transition_mode), color = XargooshTheme.colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column(Modifier.selectableGroup()) {
                    listOf(
                        TransitionMode.AUTOMATIC_GAPLESS to stringResource(R.string.automatic_gapless),
                        TransitionMode.FADE_THROUGH to stringResource(R.string.fade_through_no_overlap)
                    ).forEach { (mode, label) ->
                        AudioChoiceRow(label, audioConfig.transitionMode == mode) { audioPreferences.setTransitionMode(mode) }
                    }
                }
                if (audioConfig.transitionMode == TransitionMode.FADE_THROUGH) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.fade_duration), color = XargooshTheme.colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Column(Modifier.selectableGroup()) {
                        AudioPlaybackConfig.SUPPORTED_FADE_DURATIONS_MS.sorted().forEach { duration ->
                            AudioChoiceRow(
                                stringResource(R.string.milliseconds, duration),
                                audioConfig.fadeThroughDurationMs == duration
                            ) { audioPreferences.setFadeThroughDurationMs(duration) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().toggleable(
                        value = audioConfig.fadeUserPauseResume,
                        role = Role.Switch,
                        onValueChange = audioPreferences::setFadeUserPauseResume
                    ).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.fade_pause_resume), color = XargooshTheme.colors.onBackground, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.fade_pause_resume_description), color = XargooshTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Switch(
                        checked = audioConfig.fadeUserPauseResume,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedThumbColor = XargooshTheme.colors.primary, checkedTrackColor = XargooshTheme.colors.primary.copy(alpha = 0.5f))
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.replay_gain), color = XargooshTheme.colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column(Modifier.selectableGroup()) {
                    listOf(
                        ReplayGainMode.OFF to stringResource(R.string.replay_gain_off),
                        ReplayGainMode.TRACK to stringResource(R.string.replay_gain_track),
                        ReplayGainMode.ALBUM to stringResource(R.string.replay_gain_album)
                    ).forEach { (mode, label) ->
                        AudioChoiceRow(label, audioConfig.replayGainMode == mode) { audioPreferences.setReplayGainMode(mode) }
                    }
                }
                Text(stringResource(R.string.replay_gain_limitation), color = XargooshTheme.colors.onSurfaceVariant, fontSize = 12.sp)
            }

            var onlineLyrics by remember {
                mutableStateOf(context.getSharedPreferences("privacy_prefs", android.content.Context.MODE_PRIVATE).getBoolean("online_lyrics_enabled", false))
            }
            SettingsCard(title = stringResource(R.string.lyrics_privacy), subtitle = stringResource(R.string.optional_synchronized_lyrics)) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.automatic_online_fallback), color = XargooshTheme.colors.onBackground, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.online_lyrics_description),
                            color = XargooshTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = onlineLyrics,
                        onCheckedChange = {
                            onlineLyrics = it
                            context.getSharedPreferences("privacy_prefs", android.content.Context.MODE_PRIVATE)
                                .edit().putBoolean("online_lyrics_enabled", it).apply()
                        }
                    )
                }
                TextButton(onClick = { showPrivacyPolicy = true }) {
                    Text(stringResource(R.string.view_privacy_summary))
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsCard(title = stringResource(R.string.storage_permissions), subtitle = stringResource(R.string.manage_media_access)) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .apply { data = Uri.fromParts("package", context.packageName, null) }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, XargooshTheme.colors.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = XargooshTheme.colors.primary)
                ) {
                    Text(stringResource(R.string.manage_permissions), fontWeight = FontWeight.SemiBold)
                }
            }

            SettingsCard(title = stringResource(R.string.about_xargoosh), subtitle = null) {
                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(XargooshTheme.colors.primary.copy(alpha = 0.15f))
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.brand_monogram), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = XargooshTheme.colors.primary)
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.app_name), color = XargooshTheme.colors.onBackground, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.version_name), color = XargooshTheme.colors.onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = XargooshTheme.colors.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))
                AboutRow(label = stringResource(R.string.developer), value = stringResource(R.string.developer_name))
                AboutRow(label = stringResource(R.string.platform), value = stringResource(R.string.platform_android))
                AboutRow(label = stringResource(R.string.audio_engine), value = stringResource(R.string.audio_engine_value))
                AboutRow(label = stringResource(R.string.build), value = stringResource(R.string.build_value))
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = XargooshTheme.colors.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.about_description),
                    color = XargooshTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.copyright),
                        color = XargooshTheme.colors.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.built_with),
                        color = XargooshTheme.colors.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    com.example.xargoosh.core.components.surface.GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = XargooshTheme.shapes.large,
        color = XargooshTheme.colors.surface,
        borderColor = XargooshTheme.colors.glassBorder
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = XargooshTheme.colors.primary, fontSize = 15.sp)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = XargooshTheme.colors.onSurfaceVariant, fontSize = 12.sp)
            }
            content()
        }
    }
}

@Composable
private fun ExpandableSettingsCard(
    title: String,
    subtitle: String?,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    com.example.xargoosh.core.components.surface.GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = XargooshTheme.shapes.large,
        color = XargooshTheme.colors.surface,
        borderColor = XargooshTheme.colors.glassBorder
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = XargooshTheme.colors.primary, fontSize = 15.sp)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = XargooshTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = XargooshTheme.colors.onSurfaceVariant
            )
        }
        
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                content()
            }
        }
      }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = XargooshTheme.colors.onSurfaceVariant, fontSize = 13.sp)
        Text(value, color = XargooshTheme.colors.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AudioChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = onClick
        ).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = XargooshTheme.colors.primary)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            color = if (selected) XargooshTheme.colors.primary else XargooshTheme.colors.onBackground,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(XargooshTheme.shapes.medium).background(XargooshTheme.colors.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = XargooshTheme.colors.primary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = XargooshTheme.colors.onSurface, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = XargooshTheme.colors.onSurfaceVariant, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = XargooshTheme.colors.onSurfaceVariant)
    }
}

