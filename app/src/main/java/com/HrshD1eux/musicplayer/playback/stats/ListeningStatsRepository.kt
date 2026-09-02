/*
 * Copyright (c) 2026 Music Player Project
 * ListeningStatsRepository.kt is part of Music Player.
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

import android.content.Context
import com.HrshD1eux.musicplayer.music.resolve
import com.HrshD1eux.musicplayer.playback.persist.PlaybackHistoryDao
import com.HrshD1eux.musicplayer.playback.persist.PlaybackHistoryEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.oxycblt.musikr.Song
import timber.log.Timber as L

data class ListeningPeriodStats(val playCount: Int, val totalDurationMs: Long)

data class ListeningStats(
    val day: ListeningPeriodStats,
    val week: ListeningPeriodStats,
    val month: ListeningPeriodStats,
    val year: ListeningPeriodStats,
    val allTime: ListeningPeriodStats,
)

interface ListeningStatsRepository {
    val statsFlow: Flow<ListeningStats>

    suspend fun getStats(): ListeningStats

    suspend fun recordPlayback(song: Song, durationPlayedMs: Long)
}

@Singleton
class ListeningStatsRepositoryImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val playbackHistoryDao: PlaybackHistoryDao,
) : ListeningStatsRepository {

    private fun periodFlow(since: Long): Flow<ListeningPeriodStats> =
        combine(
            playbackHistoryDao.getPlayCountSinceFlow(since),
            playbackHistoryDao.getTotalDurationSinceFlow(since),
        ) { count: Int, duration: Long ->
            ListeningPeriodStats(count, duration)
        }

    override val statsFlow: Flow<ListeningStats> =
        combine(
                periodFlow(getStartOfDay()),
                periodFlow(getStartOfWeek()),
                periodFlow(getStartOfMonth()),
                periodFlow(getStartOfYear()),
                periodFlow(0L),
            ) {
                day: ListeningPeriodStats,
                week: ListeningPeriodStats,
                month: ListeningPeriodStats,
                year: ListeningPeriodStats,
                allTime: ListeningPeriodStats ->
                ListeningStats(day, week, month, year, allTime)
            }
            .flowOn(Dispatchers.IO)

    override suspend fun getStats(): ListeningStats {
        val startOfDay = getStartOfDay()
        val startOfWeek = getStartOfWeek()
        val startOfMonth = getStartOfMonth()
        val startOfYear = getStartOfYear()

        val dayCount = playbackHistoryDao.getPlayCountSince(startOfDay)
        val dayDur = playbackHistoryDao.getTotalDurationSince(startOfDay)

        val weekCount = playbackHistoryDao.getPlayCountSince(startOfWeek)
        val weekDur = playbackHistoryDao.getTotalDurationSince(startOfWeek)

        val monthCount = playbackHistoryDao.getPlayCountSince(startOfMonth)
        val monthDur = playbackHistoryDao.getTotalDurationSince(startOfMonth)

        val yearCount = playbackHistoryDao.getPlayCountSince(startOfYear)
        val yearDur = playbackHistoryDao.getTotalDurationSince(startOfYear)

        val allCount = playbackHistoryDao.getPlayCountSince(0L)
        val allDur = playbackHistoryDao.getTotalDurationSince(0L)

        return ListeningStats(
            day = ListeningPeriodStats(dayCount, dayDur),
            week = ListeningPeriodStats(weekCount, weekDur),
            month = ListeningPeriodStats(monthCount, monthDur),
            year = ListeningPeriodStats(yearCount, yearDur),
            allTime = ListeningPeriodStats(allCount, allDur),
        )
    }

    override suspend fun recordPlayback(song: Song, durationPlayedMs: Long) {
        if (durationPlayedMs < 3000L) {
            return
        }

        try {
            val songTitle = song.name.resolve(context)
            val artistName =
                song.artists.firstOrNull()?.name?.resolve(context)
                    ?: song.album.artists.firstOrNull()?.name?.resolve(context)
                    ?: ""

            val entry =
                PlaybackHistoryEntry(
                    songUid = song.uid,
                    songTitle = songTitle,
                    artistName = artistName,
                    durationPlayedMs = durationPlayedMs,
                    songDurationMs = song.durationMs,
                    timestamp = System.currentTimeMillis(),
                )
            playbackHistoryDao.insert(entry)
            L.d("Recorded playback of $songTitle for ${durationPlayedMs}ms")
        } catch (e: Exception) {
            L.e("Failed to record playback history: $e")
        }
    }

    private fun getStartOfDay(): Long {
        return Calendar.getInstance()
            .apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            .timeInMillis
    }

    private fun getStartOfWeek(): Long {
        return Calendar.getInstance()
            .apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            }
            .timeInMillis
    }

    private fun getStartOfMonth(): Long {
        return Calendar.getInstance()
            .apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            .timeInMillis
    }

    private fun getStartOfYear(): Long {
        return Calendar.getInstance()
            .apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_YEAR, 1)
            }
            .timeInMillis
    }
}
