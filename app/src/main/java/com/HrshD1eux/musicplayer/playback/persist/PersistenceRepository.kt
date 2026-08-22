/*
 * Copyright (c) 2023 Music Player Project
 * PersistenceRepository.kt is part of Music Player.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.HrshD1eux.musicplayer.playback.persist

import javax.inject.Inject
import com.HrshD1eux.musicplayer.music.MusicRepository
import com.HrshD1eux.musicplayer.playback.state.PlaybackStateManager
import org.oxycblt.musikr.MusicParent
import timber.log.Timber as L

/**
 * Manages the persisted playback state in a structured manner.
 *
 * @author HrshD1eux
 */
interface PersistenceRepository {
    /** Read the previously persisted [PlaybackStateManager.SavedState]. */
    suspend fun readState(): PlaybackStateManager.SavedState?

    /**
     * Persist a new [PlaybackStateManager.SavedState].
     *
     * @param state The [PlaybackStateManager.SavedState] to persist.
     */
    suspend fun saveState(state: PlaybackStateManager.SavedState?): Boolean
}

class PersistenceRepositoryImpl
@Inject
constructor(
    private val playbackStateDao: PlaybackStateDao,
    private val queueDao: QueueDao,
    private val musicRepository: MusicRepository,
) : PersistenceRepository {

    override suspend fun readState(): PlaybackStateManager.SavedState? {
        val library = musicRepository.library?.takeIf { !it.empty() } ?: return null
        val playbackState: PlaybackState
        val heapItems: List<QueueHeapItem>
        val mappingItems: List<QueueShuffledMappingItem>
        try {
            playbackState = playbackStateDao.getState() ?: return null
            heapItems = queueDao.getHeap()
            mappingItems = queueDao.getShuffledMapping()
        } catch (e: Exception) {
            L.e("Unable read playback state")
            L.e(e.stackTraceToString())
            return null
        }

        val heap = heapItems.map { library.findSong(it.uid) }
        val shuffledMapping = mappingItems.map { it.index }
        val parent = playbackState.parentUid?.let { musicRepository.find(it) as? MusicParent }

        return PlaybackStateManager.SavedState(
            positionMs = playbackState.positionMs,
            repeatMode = playbackState.repeatMode,
            parent = parent,
            heap = heap,
            shuffledMapping = shuffledMapping,
            index = playbackState.index,
            songUid = playbackState.songUid,
        )
    }

    override suspend fun saveState(state: PlaybackStateManager.SavedState?): Boolean {
        if (state == null) {
            try {
                playbackStateDao.nukeState()
                queueDao.nukeHeap()
                queueDao.nukeShuffledMapping()
                L.d("Successfully cleared previous state")
                return true
            } catch (e: Exception) {
                L.e("Unable to clear previous state")
                L.e(e.stackTraceToString())
                return false
            }
        }

        // Transform saved state into raw state, which can then be written to the database.
        val playbackState =
            PlaybackState(
                id = 0,
                index = state.index,
                positionMs = state.positionMs,
                repeatMode = state.repeatMode,
                songUid = state.songUid,
                parentUid = state.parent?.uid,
            )

        // Convert the remaining queue information to their database-specific counterparts.
        val heap =
            state.heap.mapIndexed { i, song -> QueueHeapItem(i, requireNotNull(song).uid) }

        val shuffledMapping =
            state.shuffledMapping.mapIndexed { i, index -> QueueShuffledMappingItem(i, index) }

        return try {
            playbackStateDao.replaceState(playbackState)
            queueDao.replaceQueue(heap, shuffledMapping)
            L.d("Successfully wrote new state atomically")
            true
        } catch (e: Exception) {
            L.e("Unable to write new state")
            L.e(e.stackTraceToString())
            false
        }
    }
}
