package com.example.xargoosh.domain.queue

import android.content.Context
import com.example.xargoosh.domain.models.MusicTrack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class QueueItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val track: MusicTrack
)

data class QueueRestoreState(
    val queue: List<QueueItem>,
    val normalQueue: List<QueueItem>,
    val track: MusicTrack?,
    val currentItemId: String?,
    val position: Long,
    val shuffleEnabled: Boolean
)

object QueueManager {
    private val _currentQueue = MutableStateFlow<List<QueueItem>>(emptyList())
    val currentQueue: StateFlow<List<QueueItem>> = _currentQueue.asStateFlow()

    private var normalQueue: List<QueueItem> = emptyList()
    private val mutex = Mutex()
    private val gson = Gson()

    suspend fun restoreState(context: Context): QueueRestoreState = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("xargoosh_prefs", Context.MODE_PRIVATE)
        val queueJson = prefs.getString("current_queue", null)
        val trackJson = prefs.getString("current_track", null)
        val normalQueueJson = prefs.getString("normal_queue", null)

        var restoredQueue: List<QueueItem> = emptyList()
        var restoredNormalQueue: List<QueueItem> = emptyList()
        var restoredTrack: MusicTrack? = null

        mutex.withLock {
            if (queueJson != null) {
                try {
                    val typeQueueItem = object : TypeToken<List<QueueItem>>() {}.type
                    val parsed: List<QueueItem> = gson.fromJson(queueJson, typeQueueItem)
                    if (parsed.isNotEmpty()) {
                        restoredQueue = parsed
                    }
                } catch (e: Exception) {
                    try {
                        val typeTrack = object : TypeToken<List<MusicTrack>>() {}.type
                        val oldQueue: List<MusicTrack> = gson.fromJson(queueJson, typeTrack)
                        restoredQueue = oldQueue.map { QueueItem(track = it) }
                    } catch (e2: Exception) {}
                }

                _currentQueue.value = restoredQueue
                normalQueue = runCatching {
                    val type = object : TypeToken<List<QueueItem>>() {}.type
                    gson.fromJson<List<QueueItem>>(normalQueueJson, type)
                }.getOrNull().orEmpty().ifEmpty { restoredQueue }
                restoredNormalQueue = normalQueue
            }
            if (trackJson != null) {
                try {
                    restoredTrack = gson.fromJson(trackJson, MusicTrack::class.java)
                } catch (e: Exception) {}
            }
        }
        QueueRestoreState(
            queue = restoredQueue,
            normalQueue = restoredNormalQueue,
            track = restoredTrack,
            currentItemId = prefs.getString("current_item_id", null),
            position = prefs.getLong("current_pos", 0L),
            shuffleEnabled = prefs.getBoolean("shuffle_enabled", false)
        )
    }

    suspend fun saveState(
        context: Context,
        track: MusicTrack?,
        position: Long,
        currentItemId: String?,
        shuffleEnabled: Boolean
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val q = _currentQueue.value
            val prefs = context.getSharedPreferences("xargoosh_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .apply {
                    if (q.isEmpty()) {
                        remove("current_queue")
                        remove("current_track")
                        remove("current_pos")
                        remove("current_item_id")
                        remove("normal_queue")
                        remove("shuffle_enabled")
                    } else {
                        putString("current_queue", gson.toJson(q))
                        putString("normal_queue", gson.toJson(normalQueue))
                        putString("current_track", if (track != null) gson.toJson(track) else null)
                        putLong("current_pos", position)
                        putString("current_item_id", currentItemId)
                        putBoolean("shuffle_enabled", shuffleEnabled)
                    }
                }
                .commit()
        }
    }

    suspend fun setQueue(tracks: List<MusicTrack>, startIndex: Int, isShuffle: Boolean): Pair<List<QueueItem>, Int> {
        return mutex.withLock {
            val qItems = tracks.map { QueueItem(track = it) }
            normalQueue = qItems

            val finalQueue = if (isShuffle && qItems.isNotEmpty()) {
                val first = qItems[startIndex]
                val rest = qItems.filterIndexed { index, _ -> index != startIndex }.shuffled()
                listOf(first) + rest
            } else {
                qItems
            }
            _currentQueue.value = finalQueue
            Pair(finalQueue, if (isShuffle) 0 else startIndex)
        }
    }

    suspend fun addNext(track: MusicTrack, currentItemId: String?): Pair<List<QueueItem>, QueueItem> {
        return mutex.withLock {
            val qItem = QueueItem(track = track)
            val newQueue = _currentQueue.value.toMutableList()
            val currentIndex = newQueue.indexOfFirst { it.id == currentItemId }.coerceAtLeast(0)
            val insertIndex = (currentIndex + 1).coerceIn(0, newQueue.size)
            newQueue.add(insertIndex, qItem)
            _currentQueue.value = newQueue

            val newNormalQueue = normalQueue.toMutableList()
            val normalIndex = newNormalQueue.indexOfFirst { it.id == currentItemId }
            newNormalQueue.add((normalIndex + 1).coerceIn(0, newNormalQueue.size), qItem)
            normalQueue = newNormalQueue

            Pair(newQueue, qItem)
        }
    }

    suspend fun addToEnd(track: MusicTrack): Pair<List<QueueItem>, QueueItem> {
        return mutex.withLock {
            val qItem = QueueItem(track = track)
            _currentQueue.value = _currentQueue.value + qItem
            normalQueue = normalQueue + qItem
            Pair(_currentQueue.value, qItem)
        }
    }

    suspend fun toggleShuffle(isShuffle: Boolean, currentId: String?): List<QueueItem> {
        return mutex.withLock {
            if (isShuffle) {
                val currentItem = _currentQueue.value.find { it.id == currentId }
                val newQueue = if (currentItem != null) {
                    val rest = _currentQueue.value.filter { it.id != currentItem.id }.shuffled()
                    listOf(currentItem) + rest
                } else {
                    _currentQueue.value.shuffled()
                }
                _currentQueue.value = newQueue
            } else {
                _currentQueue.value = normalQueue
            }
            _currentQueue.value
        }
    }

    data class NextResult(
        val queue: List<QueueItem>,
        val item: QueueItem,
        val wasAdded: Boolean,
        val changed: Boolean = true
    )

    suspend fun putNext(
        track: MusicTrack,
        currentItemId: String?,
        shuffleEnabled: Boolean
    ): NextResult = mutex.withLock {
        val queue = _currentQueue.value.toMutableList()
        val currentItem = queue.firstOrNull { it.id == currentItemId }
        if (currentItem?.track?.uri == track.uri) {
            return@withLock NextResult(queue, currentItem, wasAdded = false, changed = false)
        }
        val existingIndex = queue.indexOfFirst { it.track.uri == track.uri && it.id != currentItemId }
        val wasAdded = existingIndex < 0
        val item = if (existingIndex >= 0) queue.removeAt(existingIndex) else QueueItem(track = track)
        val currentIndex = queue.indexOfFirst { it.id == currentItemId }.coerceAtLeast(0)
        queue.add((currentIndex + 1).coerceAtMost(queue.size), item)
        _currentQueue.value = queue

        if (shuffleEnabled) {
            if (wasAdded) normalQueue = normalQueue + item
        } else {
            normalQueue = queue
        }
        NextResult(queue, item, wasAdded)
    }

    suspend fun applyPlaybackOrder(orderedIds: List<String>): List<QueueItem> = mutex.withLock {
        val byId = normalQueue.associateBy { it.id }
        val ordered = orderedIds.mapNotNull(byId::get)
        val included = orderedIds.toSet()
        _currentQueue.value = ordered + normalQueue.filterNot { it.id in included }
        _currentQueue.value
    }

    suspend fun restoreNormalOrder(): List<QueueItem> = mutex.withLock {
        _currentQueue.value = normalQueue
        _currentQueue.value
    }

    suspend fun removeAt(index: Int): QueueItem? {
        return mutex.withLock {
            if (index < 0 || index >= _currentQueue.value.size) return@withLock null
            val newQueue = _currentQueue.value.toMutableList()
            val removed = newQueue.removeAt(index)
            _currentQueue.value = newQueue

            val newNormal = normalQueue.toMutableList()
            newNormal.removeAll { it.id == removed.id }
            normalQueue = newNormal
            removed
        }
    }

    suspend fun move(fromIndex: Int, toIndex: Int, isShuffle: Boolean) {
        mutex.withLock {
            if (fromIndex < 0 || toIndex < 0 || fromIndex >= _currentQueue.value.size || toIndex >= _currentQueue.value.size) return@withLock
            val newQueue = _currentQueue.value.toMutableList()
            val item = newQueue.removeAt(fromIndex)
            newQueue.add(toIndex, item)
            _currentQueue.value = newQueue
            if (!isShuffle) {
                normalQueue = newQueue
            }
        }
    }

    suspend fun clear() {
        mutex.withLock {
            _currentQueue.value = emptyList()
            normalQueue = emptyList()
        }
    }

    suspend fun updateTracks(tracksByUri: Map<String, MusicTrack>): List<QueueItem> = mutex.withLock {
        if (tracksByUri.isEmpty()) return@withLock _currentQueue.value
        fun updated(items: List<QueueItem>) = items.map { item ->
            tracksByUri[item.track.uri]?.let { item.copy(track = it) } ?: item
        }
        _currentQueue.value = updated(_currentQueue.value)
        normalQueue = updated(normalQueue)
        _currentQueue.value
    }
}
