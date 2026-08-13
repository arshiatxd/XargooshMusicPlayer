package com.example.xargoosh.core.visualizer.effects

import com.example.xargoosh.core.components.surface.AeroIconButton as IconButton

import com.example.xargoosh.core.design.themes.XargooshTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xargoosh.domain.visualizer.VisualizerSettings
import com.example.xargoosh.domain.visualizer.VisualizerStyle
import com.example.xargoosh.feature.player.presentation.PlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import com.example.xargoosh.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizerSettingsScreen(
    onNavigateBack: () -> Unit,
    settings: VisualizerSettings,
    viewModel: PlayerViewModel
) {
    val coroutineScope = rememberCoroutineScope()

    val enabled by settings.enabled.collectAsStateWithLifecycle(initialValue = false)
    val placementBehindLyrics by settings.placementBehindLyrics.collectAsStateWithLifecycle(initialValue = true)
    val currentStyle by settings.style.collectAsStateWithLifecycle(initialValue = VisualizerStyle.NIER_WAVE)
    val intensity by settings.intensity.collectAsStateWithLifecycle(initialValue = 0.7f)
    val blurStrength by settings.blurStrength.collectAsStateWithLifecycle(initialValue = 0.5f)
    val renderSize by settings.renderSize.collectAsStateWithLifecycle(initialValue = 1f)
    val reduceMotion by settings.reduceMotion.collectAsStateWithLifecycle(initialValue = false)

    val overrideAutoColor by settings.overrideAutoColor.collectAsStateWithLifecycle(initialValue = false)
    val manualColorArgb by settings.manualColorArgb.collectAsStateWithLifecycle(initialValue = android.graphics.Color.RED)
    var intensityUi by remember { mutableFloatStateOf(intensity) }
    var blurStrengthUi by remember { mutableFloatStateOf(blurStrength) }
    var renderSizeUi by remember { mutableFloatStateOf(renderSize) }
    LaunchedEffect(intensity) { intensityUi = intensity }
    LaunchedEffect(blurStrength) { blurStrengthUi = blurStrength }
    LaunchedEffect(renderSize) { renderSizeUi = renderSize }

    val surfaceColor = XargooshTheme.colors.surfaceVariant.copy(alpha = 0.5f)
    val selectedBgColor = XargooshTheme.colors.surfaceVariant

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.visualizer), style = XargooshTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        intensityUi = 0.7f
                        blurStrengthUi = 0.5f
                        renderSizeUi = 1f
                        viewModel.resetVisualizerEffects()
                    }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Refresh, contentDescription = stringResource(R.string.reset_effects), tint = XargooshTheme.colors.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = XargooshTheme.colors.background
                )
            )
        },
        containerColor = XargooshTheme.colors.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }


            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceColor, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.enable_visualizer), style = XargooshTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                        Switch(
                            checked = enabled,
                            onCheckedChange = { coroutineScope.launch { settings.setEnabled(it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = XargooshTheme.colors.onPrimary,
                                checkedTrackColor = XargooshTheme.colors.primary,
                                uncheckedThumbColor = XargooshTheme.colors.onSurfaceVariant,
                                uncheckedTrackColor = XargooshTheme.colors.surfaceVariant
                            )
                        )
                    }
                    Text(
                        stringResource(R.string.visualizer_permission_explanation),
                        style = XargooshTheme.typography.bodySmall,
                        color = XargooshTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }


            item {
                Column {
                    Text(stringResource(R.string.placement), style = XargooshTheme.typography.titleSmall, color = XargooshTheme.colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceColor, RoundedCornerShape(16.dp))
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (placementBehindLyrics) selectedBgColor else Color.Transparent, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .clickable { coroutineScope.launch { settings.setPlacementBehindLyrics(true) } }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = placementBehindLyrics,
                                onClick = { coroutineScope.launch { settings.setPlacementBehindLyrics(true) } },
                                colors = RadioButtonDefaults.colors(selectedColor = XargooshTheme.colors.primary, unselectedColor = XargooshTheme.colors.onSurfaceVariant)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(stringResource(R.string.behind_lyrics), style = XargooshTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                Text(stringResource(R.string.behind_lyrics_description), style = XargooshTheme.typography.bodySmall, color = XargooshTheme.colors.onSurfaceVariant)
                            }
                        }


                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (!placementBehindLyrics) selectedBgColor else Color.Transparent, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                                .clickable { coroutineScope.launch { settings.setPlacementBehindLyrics(false) } }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !placementBehindLyrics,
                                onClick = { coroutineScope.launch { settings.setPlacementBehindLyrics(false) } },
                                colors = RadioButtonDefaults.colors(selectedColor = XargooshTheme.colors.primary, unselectedColor = XargooshTheme.colors.onSurfaceVariant)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(stringResource(R.string.fullscreen_only), style = XargooshTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                Text(stringResource(R.string.fullscreen_only_description), style = XargooshTheme.typography.bodySmall, color = XargooshTheme.colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceColor, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.reduce_motion), style = XargooshTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                        Text(
                            stringResource(R.string.reduce_motion_description),
                            style = XargooshTheme.typography.bodySmall,
                            color = XargooshTheme.colors.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(
                        checked = reduceMotion,
                        onCheckedChange = { coroutineScope.launch { settings.setReduceMotion(it) } }
                    )
                }
            }

            item {
                Column {
                    Text(stringResource(R.string.style), style = XargooshTheme.typography.titleSmall, color = XargooshTheme.colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    
                    val styles = listOf(
                        VisualizerStyle.NIER_WAVE to Icons.Filled.Waves,
                        VisualizerStyle.CIRCULAR_SPECTRUM to Icons.Filled.MotionPhotosOn,
                        VisualizerStyle.SPECTRUM_BARS to Icons.Filled.GraphicEq,
                        VisualizerStyle.PARTICLES to Icons.Filled.ScatterPlot,
                        VisualizerStyle.DUAL_WAVE to Icons.Filled.Waves,
                        VisualizerStyle.MINIMAL_LINE to Icons.Filled.PlayCircleOutline
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(styles) { (style, icon) ->
                            val isSelected = currentStyle == style
                            StyleItem(
                                style = style,
                                icon = icon,
                                isSelected = isSelected,
                                onClick = { coroutineScope.launch { settings.setStyle(style) } }
                            )
                        }
                    }
                }
            }


            item {
                Column {
                    Text(stringResource(R.string.intensity), style = XargooshTheme.typography.titleSmall, color = XargooshTheme.colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = intensityUi,
                            onValueChange = { intensityUi = it },
                            onValueChangeFinished = { viewModel.setVisualizerIntensity(intensityUi) },
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = XargooshTheme.colors.primary,
                                activeTrackColor = XargooshTheme.colors.primary,
                                inactiveTrackColor = XargooshTheme.colors.surfaceVariant
                            )
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.percentage, (intensityUi * 100).roundToInt()),
                            style = XargooshTheme.typography.bodyMedium,
                            color = XargooshTheme.colors.onSurfaceVariant
                        )
                    }
                }
            }


            item {
                Column {
                    Text(stringResource(R.string.blur_strength), style = XargooshTheme.typography.titleSmall, color = XargooshTheme.colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = blurStrengthUi,
                            onValueChange = { blurStrengthUi = it },
                            onValueChangeFinished = { viewModel.setVisualizerBlurStrength(blurStrengthUi) },
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = XargooshTheme.colors.primary,
                                activeTrackColor = XargooshTheme.colors.primary,
                                inactiveTrackColor = XargooshTheme.colors.surfaceVariant
                            )
                        )
                        Spacer(Modifier.width(16.dp))
                        val blurText = when {
                            blurStrengthUi < 0.3f -> stringResource(R.string.low)
                            blurStrengthUi < 0.7f -> stringResource(R.string.medium_abbreviation)
                            else -> stringResource(R.string.high)
                        }
                        Text(
                            text = blurText,
                            style = XargooshTheme.typography.bodyMedium,
                            color = XargooshTheme.colors.onSurfaceVariant,
                            modifier = Modifier.defaultMinSize(minWidth = 36.dp)
                        )
                    }
                }
            }

            item {
                Column {
                    Text(stringResource(R.string.visualizer_size), style = XargooshTheme.typography.titleSmall, color = XargooshTheme.colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = renderSizeUi,
                            onValueChange = { renderSizeUi = it },
                            onValueChangeFinished = { viewModel.setVisualizerRenderSize(renderSizeUi) },
                            valueRange = 0.6f..2f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = XargooshTheme.colors.primary,
                                activeTrackColor = XargooshTheme.colors.primary,
                                inactiveTrackColor = XargooshTheme.colors.surfaceVariant
                            )
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.percentage, (renderSizeUi * 100).roundToInt()), color = XargooshTheme.colors.onSurfaceVariant, modifier = Modifier.defaultMinSize(minWidth = 44.dp))
                    }
                }
            }


            item {
                Column {
                    Text(stringResource(R.string.visualizer_color), style = XargooshTheme.typography.titleSmall, color = XargooshTheme.colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceColor, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.auto_album_palette), style = XargooshTheme.typography.bodyLarge)
                            Switch(
                                checked = !overrideAutoColor,
                                onCheckedChange = { coroutineScope.launch { settings.setOverrideAutoColor(!it) } },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = XargooshTheme.colors.onPrimary,
                                    checkedTrackColor = XargooshTheme.colors.primary,
                                    uncheckedThumbColor = XargooshTheme.colors.onSurfaceVariant,
                                    uncheckedTrackColor = XargooshTheme.colors.surfaceVariant
                                )
                            )
                        }

                        if (overrideAutoColor) {
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.presets), style = XargooshTheme.typography.bodySmall, color = XargooshTheme.colors.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            val presetColors = listOf(
                                Color.Red, Color(0xFF00FF00), Color.Blue, 
                                Color.Cyan, Color.Magenta, Color.Yellow, Color.White
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(presetColors) { color ->
                                    val isSelected = manualColorArgb == color.toArgb()
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) XargooshTheme.colors.onSurface else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                coroutineScope.launch { settings.setManualColorArgb(color.toArgb()) }
                                            }
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.manual_rgb), style = XargooshTheme.typography.bodySmall, color = XargooshTheme.colors.onSurfaceVariant)
                            
                            val c = Color(manualColorArgb)
                            var r by remember(c) { mutableStateOf(c.red) }
                            var g by remember(c) { mutableStateOf(c.green) }
                            var b by remember(c) { mutableStateOf(c.blue) }

                            val updateColor = { 
                                coroutineScope.launch { 
                                    settings.setManualColorArgb(Color(r, g, b, 1f).toArgb()) 
                                } 
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.red_abbreviation), color = Color.Red, modifier = Modifier.width(24.dp))
                                Slider(value = r, onValueChange = { r = it }, onValueChangeFinished = { updateColor() }, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.green_abbreviation), color = Color.Green, modifier = Modifier.width(24.dp))
                                Slider(value = g, onValueChange = { g = it }, onValueChangeFinished = { updateColor() }, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.blue_abbreviation), color = Color.Blue, modifier = Modifier.width(24.dp))
                                Slider(value = b, onValueChange = { b = it }, onValueChangeFinished = { updateColor() }, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue))
                            }
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun StyleItem(
    style: VisualizerStyle,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (isSelected) XargooshTheme.colors.primary else XargooshTheme.colors.onSurfaceVariant
    

    val displayName = when (style) {
        VisualizerStyle.CIRCULAR_SPECTRUM -> stringResource(R.string.style_circle)
        VisualizerStyle.MINIMAL_LINE -> stringResource(R.string.style_minimal)
        else -> stringResource(style.displayNameRes)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(XargooshTheme.colors.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) XargooshTheme.colors.primary else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = displayName,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = displayName,
            style = XargooshTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = color,
            maxLines = 1
        )
    }
}


