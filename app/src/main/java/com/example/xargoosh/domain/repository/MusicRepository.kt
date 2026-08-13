package com.example.xargoosh.domain.repository

import com.example.xargoosh.domain.models.MusicTrack

interface MusicRepository {
    suspend fun getLocalAudioFiles(): List<MusicTrack>
    suspend fun getTrackById(id: String): MusicTrack?
}
