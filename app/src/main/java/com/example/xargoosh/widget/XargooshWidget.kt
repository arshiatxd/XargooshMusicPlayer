package com.example.xargoosh.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.palette.graphics.Palette
import com.example.xargoosh.MainActivity
import com.example.xargoosh.R
import com.example.xargoosh.domain.models.MusicTrack
import com.google.gson.Gson

enum class WidgetLayout { COMPACT, WIDE, SQUARE }

private data class WidgetState(
    val title: String,
    val artist: String,
    val artwork: Bitmap?,
    val isPlaying: Boolean,
    val accent: Color
)

open class XargooshWidget(
    private val layout: WidgetLayout = WidgetLayout.SQUARE
) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("xargoosh_prefs", Context.MODE_PRIVATE)
        val track = prefs.getString("current_track", null)?.let { json ->
            runCatching { Gson().fromJson(json, MusicTrack::class.java) }.getOrNull()
        }
        val artwork = track?.uri?.let { extractArtwork(context, it) }
        val accent = artwork?.let {
            Color(Palette.from(it).generate().getVibrantColor(0xFFFF4D67.toInt()))
        } ?: Color(0xFFFF4D67)
        val state = WidgetState(
            title = track?.title ?: context.getString(R.string.brand_xargoosh),
            artist = track?.artist ?: context.getString(R.string.tap_to_open),
            artwork = artwork,
            isPlaying = prefs.getBoolean("widget_is_playing", false),
            accent = accent
        )

        provideContent {
            GlanceTheme {
                when (layout) {
                    WidgetLayout.COMPACT -> CompactWidget(context, state)
                    WidgetLayout.WIDE -> WideWidget(context, state)
                    WidgetLayout.SQUARE -> SquareWidget(context, state)
                }
            }
        }
    }

    private fun extractArtwork(context: Context, uri: String): Bitmap? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(uri))
            val bytes = retriever.embeddedPicture ?: return@runCatching null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > 384 || bounds.outHeight / sampleSize > 384) sampleSize *= 2
            BitmapFactory.decodeByteArray(
                bytes, 0, bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sampleSize }
            )
        } finally {
            retriever.release()
        }
    }.getOrNull()
}

private val widgetBackground = ColorProvider(Color(0xF21A1A1F))
private val primaryText = ColorProvider(Color.White)
private val secondaryText = ColorProvider(Color(0xFFCAC6CF))

@Composable
private fun CompactWidget(context: Context, state: WidgetState) {
    Row(
        modifier = widgetRoot(6),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(context, state.artwork, 48)
        Spacer(GlanceModifier.width(10.dp))
        TrackText(state, GlanceModifier.defaultWeight())
        PlaybackControls(context, state, compact = true)
    }
}

@Composable
private fun WideWidget(context: Context, state: WidgetState) {
    Row(
        modifier = widgetRoot(10),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(context, state.artwork, 76)
        Spacer(GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            TrackText(state, GlanceModifier.fillMaxWidth())
            Spacer(GlanceModifier.height(8.dp))
            PlaybackControls(context, state, compact = false)
        }
    }
}

@Composable
private fun SquareWidget(context: Context, state: WidgetState) {
    Column(
        modifier = widgetRoot(10),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(context, state.artwork, 112)
        Spacer(GlanceModifier.height(8.dp))
        TrackText(state, GlanceModifier.fillMaxWidth(), centered = true)
        Spacer(GlanceModifier.height(8.dp))
        PlaybackControls(context, state, compact = false)
    }
}

private fun widgetRoot(padding: Int) = GlanceModifier
    .fillMaxSize()
    .cornerRadius(24.dp)
    .background(widgetBackground)
    .padding(padding.dp)
    .clickable(actionStartActivity<MainActivity>())

@Composable
private fun Artwork(context: Context, bitmap: Bitmap?, size: Int) {
    Box(
        modifier = GlanceModifier.size(size.dp).cornerRadius(18.dp).background(ColorProvider(Color(0xFF34323A))),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = context.getString(R.string.album_artwork),
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                provider = ImageProvider(R.drawable.default_cover),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun TrackText(state: WidgetState, modifier: GlanceModifier, centered: Boolean = false) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Text(
            text = state.title,
            style = TextStyle(color = primaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp),
            maxLines = 1
        )
        Spacer(GlanceModifier.height(3.dp))
        Text(
            text = state.artist,
            style = TextStyle(color = secondaryText, fontSize = 12.sp),
            maxLines = 1
        )
    }
}

@Composable
private fun PlaybackControls(context: Context, state: WidgetState, compact: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
        WidgetButton(context, "PREV", R.drawable.ic_widget_previous, 40, description = context.getString(R.string.previous))
        Spacer(GlanceModifier.width(if (compact) 3.dp else 8.dp))
        WidgetButton(
            context,
            "PLAY_PAUSE",
            if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            44,
            state.accent,
            context.getString(if (state.isPlaying) R.string.pause else R.string.play)
        )
        Spacer(GlanceModifier.width(if (compact) 3.dp else 8.dp))
        WidgetButton(context, "NEXT", R.drawable.ic_widget_next, 40, description = context.getString(R.string.next))
    }
}

@Composable
private fun WidgetButton(
    context: Context,
    action: String,
    icon: Int,
    size: Int,
    background: Color = Color.Transparent,
    description: String
) {
    Box(
        modifier = GlanceModifier
            .size(size.dp)
            .cornerRadius((size / 2).dp)
            .background(ColorProvider(background))
            .clickable(actionSendBroadcast(Intent(context, WidgetActionReceiver::class.java).setAction(action))),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = description,
            modifier = GlanceModifier.size((size * 0.55f).dp)
        )
    }
}

class XargooshWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = XargooshSquareWidget()
}

class XargooshWideWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = XargooshWideWidget()
}

class XargooshCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = XargooshCompactWidget()
}

class XargooshSquareWidget : XargooshWidget(WidgetLayout.SQUARE)
class XargooshWideWidget : XargooshWidget(WidgetLayout.WIDE)
class XargooshCompactWidget : XargooshWidget(WidgetLayout.COMPACT)

suspend fun updateAllXargooshWidgets(context: Context) {
    XargooshSquareWidget().updateAll(context)
    XargooshWideWidget().updateAll(context)
    XargooshCompactWidget().updateAll(context)
}
