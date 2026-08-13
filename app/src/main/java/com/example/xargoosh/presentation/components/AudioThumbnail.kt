package com.example.xargoosh.presentation.components

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.xargoosh.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private val artworkCache = object : LruCache<String, android.graphics.Bitmap>(24 * 1024) {
    override fun sizeOf(key: String, value: android.graphics.Bitmap): Int = value.byteCount / 1024
}
private val artworkRevisions = mutableStateMapOf<String, Int>()
private val artworkDecodeSemaphore = Semaphore(2)
private val missingArtwork = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
private data class ArtworkState(val loading: Boolean = true, val bitmap: android.graphics.Bitmap? = null)

fun invalidateArtwork(uri: String) {
    artworkCache.remove(uri)
    missingArtwork.remove(uri)
    artworkRevisions[uri] = (artworkRevisions[uri] ?: 0) + 1
}

private fun decodeArtwork(data: ByteArray): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > 720 || bounds.outHeight / sampleSize > 720) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeByteArray(
        data,
        0,
        data.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
    )
}

@Composable
fun AudioThumbnail(uri: String, contentScale: ContentScale = ContentScale.Crop, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val revision = artworkRevisions[uri] ?: 0
    val artworkState by produceState(initialValue = ArtworkState(), uri, revision) {
        val bitmap = withContext(Dispatchers.IO) {
            if (uri in missingArtwork) return@withContext null
            artworkCache.get(uri) ?: artworkDecodeSemaphore.withPermit {
                artworkCache.get(uri) ?: runCatching {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, Uri.parse(uri))
                        retriever.embeddedPicture?.let(::decodeArtwork)?.also { artworkCache.put(uri, it) }
                    } finally {
                        retriever.release()
                    }
                }.getOrNull().also { if (it == null) missingArtwork += uri }
            }
        }
        value = ArtworkState(loading = false, bitmap = bitmap)
    }

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(Color(0xFF0B2934), Color(0xFF11172A), Color(0xFF241542))
            )
        )
    ) {
        val artwork = artworkState.bitmap
        if (artwork != null) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else if (!artworkState.loading) {
            Image(
                painter = painterResource(R.drawable.default_cover),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
