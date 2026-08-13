package com.example.xargoosh.presentation.equalizer

import com.example.xargoosh.core.components.surface.AeroIconButton as IconButton

import android.media.audiofx.Equalizer
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xargoosh.core.design.themes.XargooshTheme
import androidx.compose.ui.res.stringResource
import com.example.xargoosh.R

object EqualizerController {
    private var effect: Equalizer? = null
    private var sessionId: Int = 0

    fun attach(audioSessionId: Int): Equalizer? {
        if (audioSessionId <= 0) {
            release()
            return null
        }
        if (effect != null && sessionId == audioSessionId) return effect
        effect?.release()
        effect = runCatching { Equalizer(0, audioSessionId).apply { enabled = true } }.getOrNull()
        sessionId = audioSessionId
        return effect
    }

    fun release() {
        runCatching { effect?.enabled = false }
        runCatching { effect?.release() }
        effect = null
        sessionId = 0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(audioSessionId: Int, onBack: () -> Unit) {
    val equalizer = remember(audioSessionId) { EqualizerController.attach(audioSessionId) }
    var refresh by remember { mutableIntStateOf(0) }
    val bands = remember(equalizer, refresh) {
        equalizer?.let { effect ->
            (0 until effect.numberOfBands.toInt()).map { index ->
                val band = index.toShort()
                Triple(band, effect.getCenterFreq(band) / 1000, effect.getBandLevel(band))
            }
        }.orEmpty()
    }
    val range = equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
    var selectedPreset by remember(audioSessionId) { mutableIntStateOf(R.string.preset_flat) }

    Scaffold(
        containerColor = XargooshTheme.colors.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.equalizer), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            bands.forEach { (band, _, _) -> equalizer?.setBandLevel(band, 0.toShort()) }
                            selectedPreset = R.string.preset_flat
                            refresh++
                        },
                        enabled = equalizer != null
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset_equalizer))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = XargooshTheme.colors.background)
            )
        }
    ) { padding ->
        if (equalizer == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = XargooshTheme.colors.primary, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.equalizer_inactive), color = XargooshTheme.colors.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    com.example.xargoosh.core.components.surface.GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = XargooshTheme.colors.surfaceVariant,
                        borderColor = XargooshTheme.colors.glassBorder
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stringResource(R.string.audio_effect), color = XargooshTheme.colors.onBackground, fontWeight = FontWeight.SemiBold)
                                Text(stringResource(R.string.band_equalizer, bands.size), color = XargooshTheme.colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = equalizer.enabled,
                                onCheckedChange = { equalizer.enabled = it; refresh++ }
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.presets), style = MaterialTheme.typography.titleSmall, color = XargooshTheme.colors.onSurface)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            equalizerPresets.forEach { preset ->
                                FilterChip(
                                    selected = selectedPreset == preset.nameRes,
                                    onClick = {
                                        bands.forEachIndexed { index, (band, _, _) ->
                                            val curveIndex = if (bands.size <= 1) 0 else index * (preset.levels.size - 1) / (bands.size - 1)
                                            val level = (preset.levels[curveIndex] * 100).toInt().coerceIn(range[0].toInt(), range[1].toInt())
                                            equalizer.setBandLevel(band, level.toShort())
                                        }
                                        selectedPreset = preset.nameRes
                                        refresh++
                                    },
                                    label = { Text(stringResource(preset.nameRes)) }
                                )
                            }
                        }
                    }
                }

                items(bands, key = { it.first }) { (band, frequency, level) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatFrequency(frequency), modifier = Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = XargooshTheme.colors.onSurfaceVariant)
                        Slider(
                            value = level.toFloat(),
                            onValueChange = { equalizer.setBandLevel(band, it.toInt().toShort()); selectedPreset = R.string.preset_custom; refresh++ },
                            valueRange = range[0].toFloat()..range[1].toFloat(),
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = XargooshTheme.colors.primary,
                                activeTrackColor = XargooshTheme.colors.primary,
                                inactiveTrackColor = XargooshTheme.colors.surfaceVariant
                            )
                        )
                        Text(stringResource(R.string.decibels, level / 100f), modifier = Modifier.width(58.dp), style = MaterialTheme.typography.labelSmall, color = XargooshTheme.colors.primary)
                    }
                }
            }
        }
    }
}

private fun formatFrequency(frequencyHz: Int): String =
    if (frequencyHz >= 1000) "${frequencyHz / 1000}k" else frequencyHz.toString()

private data class EqualizerPreset(val nameRes: Int, val levels: List<Float>)

private val equalizerPresets = listOf(
    EqualizerPreset(R.string.preset_flat, listOf(0f, 0f, 0f, 0f, 0f)),
    EqualizerPreset(R.string.preset_bass, listOf(6f, 4f, 1f, 0f, -1f)),
    EqualizerPreset(R.string.preset_rock, listOf(4f, 2f, -1f, 2f, 4f)),
    EqualizerPreset(R.string.preset_vocal, listOf(-2f, 0f, 4f, 3f, 0f)),
    EqualizerPreset(R.string.preset_treble, listOf(-2f, -1f, 0f, 4f, 6f))
)
