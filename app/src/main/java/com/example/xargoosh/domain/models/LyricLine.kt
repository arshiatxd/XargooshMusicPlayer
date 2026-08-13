package com.example.xargoosh.domain.models

data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val isSynced: Boolean = true
)
