/*
 * Copyright (c) 2026 Music Player Project
 * ListeningTracker.kt is part of Music Player.
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
 
package com.HrshD1eux.musicplayer.playback.stats

import com.HrshD1eux.musicplayer.playback.state.PlaybackStateManager
import com.HrshD1eux.musicplayer.playback.state.Progression
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.oxycblt.musikr.MusicParent
import org.oxycblt.musikr.Song

/**
 * Tracks listening duration and play counts in real time, recording completed or sustained playback
 * sessions to the [ListeningStatsRepository].
 *
 * @author HrshD1eux
 */
@Singleton
class ListeningTracker
@Inject
constructor(
    private val playbackManager: PlaybackStateManager,
    private val statsRepository: ListeningStatsRepository,
) : PlaybackStateManager.Listener {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var activeSong: Song? = null
    private var lastPlayTimestamp: Long = 0L
    private var accumulatedDurationMs: Long = 0L
    private var isCurrentlyPlaying: Boolean = false

    fun attach() {
        playbackManager.addListener(this)
        activeSong = playbackManager.currentSong
        isCurrentlyPlaying = playbackManager.progression.isPlaying
        if (isCurrentlyPlaying) {
            lastPlayTimestamp = System.currentTimeMillis()
        }
    }

    fun release() {
        commitCurrentSession()
        playbackManager.removeListener(this)
        job.cancel()
    }

    override fun onNewPlayback(
        parent: MusicParent?,
        queue: List<Song>,
        index: Int,
        isShuffled: Boolean,
    ) {
        val newSong = queue.getOrNull(index)
        if (newSong != activeSong) {
            commitCurrentSession()
            activeSong = newSong
            if (isCurrentlyPlaying) {
                lastPlayTimestamp = System.currentTimeMillis()
            }
        }
    }

    override fun onIndexMoved(index: Int) {
        val newSong = playbackManager.queue.getOrNull(index)
        if (newSong != activeSong) {
            commitCurrentSession()
            activeSong = newSong
            if (isCurrentlyPlaying) {
                lastPlayTimestamp = System.currentTimeMillis()
            }
        }
    }

    override fun onProgressionChanged(progression: Progression) {
        val wasPlaying = isCurrentlyPlaying
        isCurrentlyPlaying = progression.isPlaying

        val now = System.currentTimeMillis()

        if (wasPlaying && !isCurrentlyPlaying) {
            // Paused: calculate elapsed time and add to accumulator
            if (lastPlayTimestamp > 0L) {
                val elapsed = now - lastPlayTimestamp
                if (elapsed in 1..MAX_TICK_DELTA_MS) {
                    accumulatedDurationMs += elapsed
                }
            }
            lastPlayTimestamp = 0L
        } else if (!wasPlaying && isCurrentlyPlaying) {
            // Resumed / Started playing
            lastPlayTimestamp = now
        }
    }

    override fun onSessionEnded() {
        commitCurrentSession()
    }

    private fun commitCurrentSession() {
        val songToRecord = activeSong
        val now = System.currentTimeMillis()

        if (isCurrentlyPlaying && lastPlayTimestamp > 0L) {
            val elapsed = now - lastPlayTimestamp
            if (elapsed in 1..MAX_TICK_DELTA_MS) {
                accumulatedDurationMs += elapsed
            }
            lastPlayTimestamp = now
        }

        val totalListened = accumulatedDurationMs
        accumulatedDurationMs = 0L

        if (songToRecord != null && totalListened >= MIN_RECORD_DURATION_MS) {
            scope.launch { statsRepository.recordPlayback(songToRecord, totalListened) }
        }
    }

    private companion object {
        private const val MIN_RECORD_DURATION_MS = 5000L
        private const val MAX_TICK_DELTA_MS = 2 * 60 * 60 * 1000L
    }
}
