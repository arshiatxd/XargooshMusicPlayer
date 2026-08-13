package com.example.xargoosh.data.local

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.TagField
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX
import org.jaudiotagger.tag.mp4.field.Mp4TagReverseDnsField
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTagField
import java.io.File
import java.util.Locale

data class ReplayGainMetadata(
    val trackDb: Float? = null,
    val trackPeak: Float? = null,
    val albumDb: Float? = null,
    val albumPeak: Float? = null
)

object ReplayGainMetadataReader {
    private const val TRACK_GAIN = "REPLAYGAIN_TRACK_GAIN"
    private const val TRACK_PEAK = "REPLAYGAIN_TRACK_PEAK"
    private const val ALBUM_GAIN = "REPLAYGAIN_ALBUM_GAIN"
    private const val ALBUM_PEAK = "REPLAYGAIN_ALBUM_PEAK"

    fun read(file: File): ReplayGainMetadata {
        if (!file.isFile || !file.canRead()) return ReplayGainMetadata()
        val values = mutableMapOf<String, String>()
        val fields = AudioFileIO.read(file).tag?.fields ?: return ReplayGainMetadata()
        while (fields.hasNext()) extract(fields.next())?.let { (key, value) -> values[normalizeTagKey(key)] = value }
        return ReplayGainMetadata(
            trackDb = parseGainDb(values[TRACK_GAIN]),
            trackPeak = parsePeak(values[TRACK_PEAK]),
            albumDb = parseGainDb(values[ALBUM_GAIN]),
            albumPeak = parsePeak(values[ALBUM_PEAK])
        )
    }

    private fun extract(field: TagField): Pair<String, String>? = when (field) {
        is AbstractID3v2Frame -> (field.body as? FrameBodyTXXX)?.let { it.description to it.text }
        is VorbisCommentTagField -> field.id to field.content
        is Mp4TagReverseDnsField -> field.descriptor to field.content
        else -> null
    }

    fun parseGainDb(value: String?): Float? {
        val cleaned = value?.trim()?.replace(Regex("(?i)\\s*dB\\s*$"), "") ?: return null
        return cleaned.toFloatOrNull()?.takeIf(Float::isFinite)
    }

    fun parsePeak(value: String?): Float? =
        value?.trim()?.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f }

    fun normalizeTagKey(key: String): String = key.uppercase(Locale.ROOT)
}
