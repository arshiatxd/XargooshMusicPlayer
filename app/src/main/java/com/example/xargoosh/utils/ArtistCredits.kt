package com.example.xargoosh.utils

import com.example.xargoosh.domain.models.MusicTrack
import java.util.Locale

object ArtistCredits {
    private val explicitSeparator = Regex(
        """(?i)\s*\b(?:feat(?:uring)?|ft|with|vs|versus)\.?\s+"""
    )
    private val creditSeparator = Regex("""\s*(?:,|&|;|\+)\s*|\s+[x×]\s+""", RegexOption.IGNORE_CASE)

    // These punctuation marks are part of the credited artist's established name.
    private val compoundNames = setOf(
        "ac/dc",
        "earth, wind & fire",
        "tyler, the creator",
        "simon & garfunkel",
        "hall & oates",
        "mumford & sons",
        "marina & the diamonds",
        "florence + the machine"
    )

    fun names(raw: String): List<String> {
        val normalized = raw.trim()
        if (normalized.isEmpty()) return emptyList()
        return explicitSeparator.split(normalized)
            .flatMap(::splitCredit)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy(::key)
            .ifEmpty { listOf(normalized) }
    }

    private fun splitCredit(credit: String): List<String> {
        var shielded = credit.trim()
        val protected = mutableMapOf<String, String>()
        compoundNames.forEachIndexed { index, compound ->
            val matcher = Regex(Regex.escape(compound), RegexOption.IGNORE_CASE)
            shielded = matcher.replace(shielded) { match ->
                val token = "__compound_${index}_${protected.size}__"
                protected[token] = match.value
                token
            }
        }
        return creditSeparator.split(shielded).map { part ->
            protected[part.trim()] ?: part
        }
    }

    fun contains(raw: String, artist: String): Boolean = names(raw).any { key(it) == key(artist) }

    fun matchScore(artist: String, query: String): Int {
        val candidate = key(artist).replace(Regex("""[^\p{L}\p{N}]+"""), " ").trim()
        val target = key(query).replace(Regex("""[^\p{L}\p{N}]+"""), " ").trim()
        if (target.isEmpty()) return 0
        if (candidate == target) return 0
        if (candidate.startsWith(target) || target.startsWith(candidate)) return 1
        val queryWords = target.split(' ').filter(String::isNotEmpty)
        if (queryWords.isNotEmpty() && queryWords.all(candidate::contains)) return 2
        if (target.length >= 4 && levenshtein(candidate, target) <= maxOf(2, candidate.length / 5)) return 2
        if (candidate.contains(target)) return 3
        return Int.MAX_VALUE
    }

    fun replace(raw: String, oldArtist: String, newArtist: String): String {
        if (!contains(raw, oldArtist)) return raw
        val escaped = Regex.escape(oldArtist.trim())
        val exactCredit = Regex("(?i)(?<![\\p{L}\\p{N}])$escaped(?![\\p{L}\\p{N}])")
        return exactCredit.replaceFirst(raw, newArtist.trim())
    }

    fun groupTracks(tracks: List<MusicTrack>): Map<String, List<MusicTrack>> {
        val groups = linkedMapOf<String, Pair<String, MutableList<MusicTrack>>>()
        val seenUris = mutableMapOf<String, MutableSet<String>>()
        tracks.forEach { track ->
            names(track.artist).forEach { artist ->
                val artistKey = key(artist)
                val entry = groups.getOrPut(artistKey) { artist to mutableListOf() }
                if (seenUris.getOrPut(artistKey) { mutableSetOf() }.add(track.uri)) entry.second += track
            }
        }
        return groups.values.associate { it.first to it.second.toList() }
    }

    private fun key(value: String): String = value.trim().lowercase(Locale.ROOT)

    private fun levenshtein(left: String, right: String): Int {
        var previous = IntArray(right.length + 1) { it }
        for (i in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = i + 1
            for (j in right.indices) {
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + if (left[i] == right[j]) 0 else 1
                )
            }
            previous = current
        }
        return previous[right.length]
    }
}
