/*
 * Copyright (c) 2023 Music Player Project
 * PersistenceDatabase.kt is part of Music Player.
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

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.HrshD1eux.musicplayer.playback.state.RepeatMode
import kotlinx.coroutines.flow.Flow
import org.oxycblt.musikr.Music

/**
 * Provides raw access to the database storing the persisted playback state and listening history.
 *
 * @author HrshD1eux
 */
@Database(
    entities =
        [
            PlaybackState::class,
            QueueHeapItem::class,
            QueueShuffledMappingItem::class,
            PlaybackHistoryEntry::class,
        ],
    version = 39,
    exportSchema = false,
)
@TypeConverters(Music.UID.TypeConverters::class)
abstract class PersistenceDatabase : RoomDatabase() {
    /**
     * Get the current [PlaybackStateDao].
     *
     * @return A [PlaybackStateDao] providing control of the database's playback state tables.
     */
    abstract fun playbackStateDao(): PlaybackStateDao

    /**
     * Get the current [QueueDao].
     *
     * @return A [QueueDao] providing control of the database's queue tables.
     */
    abstract fun queueDao(): QueueDao

    /**
     * Get the current [PlaybackHistoryDao].
     *
     * @return A [PlaybackHistoryDao] providing access to listening history.
     */
    abstract fun playbackHistoryDao(): PlaybackHistoryDao

    companion object {
        val MIGRATION_27_32 =
            Migration(27, 32) {
                // Switched from custom names to just letting room pick the names
                it.execSQL("ALTER TABLE playback_state RENAME TO PlaybackState")
                it.execSQL("ALTER TABLE queue_heap RENAME TO QueueHeapItem")
                it.execSQL("ALTER TABLE queue_mapping RENAME TO QueueMappingItem")
            }

        val MIGRATION_38_39 =
            Migration(38, 39) {
                it.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `PlaybackHistoryEntry` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `songUid` TEXT NOT NULL,
                        `songTitle` TEXT NOT NULL,
                        `artistName` TEXT NOT NULL,
                        `durationPlayedMs` INTEGER NOT NULL,
                        `songDurationMs` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """
                        .trimIndent()
                )
            }
    }
}

/**
 * Provides control of the persisted playback state table.
 *
 * @author HrshD1eux
 */
@Dao
interface PlaybackStateDao {
    /**
     * Get the previously persisted [PlaybackState].
     *
     * @return The previously persisted [PlaybackState], or null if one was not present.
     */
    @Query("SELECT * FROM PlaybackState WHERE id = 0") suspend fun getState(): PlaybackState?

    /** Delete any previously persisted [PlaybackState]s. */
    @Query("DELETE FROM PlaybackState") suspend fun nukeState()

    /**
     * Insert a new [PlaybackState] into the database.
     *
     * @param state The [PlaybackState] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertState(state: PlaybackState)

    /** Atomically replace the persisted [PlaybackState]. */
    @Transaction
    suspend fun replaceState(state: PlaybackState) {
        nukeState()
        insertState(state)
    }
}

/**
 * Provides control of the persisted queue state tables.
 *
 * @author HrshD1eux
 */
@Dao
interface QueueDao {
    /**
     * Get the previously persisted queue heap.
     *
     * @return A list of persisted [QueueHeapItem]s wrapping each heap item.
     */
    @Query("SELECT * FROM QueueHeapItem") suspend fun getHeap(): List<QueueHeapItem>

    /**
     * Get the previously persisted queue mapping.
     *
     * @return A list of persisted [QueueShuffledMappingItem]s wrapping each heap item.
     */
    @Query("SELECT * FROM QueueShuffledMappingItem")
    suspend fun getShuffledMapping(): List<QueueShuffledMappingItem>

    /** Delete any previously persisted queue heap entries. */
    @Query("DELETE FROM QueueHeapItem") suspend fun nukeHeap()

    /** Delete any previously persisted queue mapping entries. */
    @Query("DELETE FROM QueueShuffledMappingItem") suspend fun nukeShuffledMapping()

    /**
     * Insert new heap entries into the database.
     *
     * @param heap The list of wrapped [QueueHeapItem]s to insert.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertHeap(heap: List<QueueHeapItem>)

    /**
     * Insert new mapping entries into the database.
     *
     * @param mapping The list of wrapped [QueueShuffledMappingItem] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertShuffledMapping(mapping: List<QueueShuffledMappingItem>)

    /** Atomically replace the persisted queue state. */
    @Transaction
    suspend fun replaceQueue(heap: List<QueueHeapItem>, mapping: List<QueueShuffledMappingItem>) {
        nukeHeap()
        nukeShuffledMapping()
        insertHeap(heap)
        insertShuffledMapping(mapping)
    }
}

// TODO: Figure out how to get RepeatMode to map to an int instead of a string
@Entity
data class PlaybackState(
    @PrimaryKey val id: Int,
    val index: Int,
    val positionMs: Long,
    val repeatMode: RepeatMode,
    val songUid: Music.UID,
    val parentUid: Music.UID?,
)

@Entity data class QueueHeapItem(@PrimaryKey val id: Int, val uid: Music.UID)

@Entity data class QueueShuffledMappingItem(@PrimaryKey val id: Int, val index: Int)

@Entity(tableName = "PlaybackHistoryEntry")
data class PlaybackHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songUid: Music.UID,
    val songTitle: String,
    val artistName: String,
    val durationPlayedMs: Long,
    val songDurationMs: Long,
    val timestamp: Long,
)

/**
 * Provides control of the persisted playback history and listening statistics table.
 *
 * @author HrshD1eux
 */
@Dao
interface PlaybackHistoryDao {
    /**
     * Insert a new playback history entry into the database.
     *
     * @param entry The [PlaybackHistoryEntry] to insert.
     * @return The row ID of the inserted entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PlaybackHistoryEntry): Long

    /**
     * Get the count of songs played since a given timestamp.
     *
     * @param sinceTimestamp Epoch milliseconds threshold.
     */
    @Query("SELECT COUNT(*) FROM PlaybackHistoryEntry WHERE timestamp >= :sinceTimestamp")
    fun getPlayCountSinceFlow(sinceTimestamp: Long): Flow<Int>

    /**
     * Get the total duration of songs listened to since a given timestamp.
     *
     * @param sinceTimestamp Epoch milliseconds threshold.
     */
    @Query(
        "SELECT COALESCE(SUM(durationPlayedMs), 0) FROM PlaybackHistoryEntry WHERE timestamp >= :sinceTimestamp"
    )
    fun getTotalDurationSinceFlow(sinceTimestamp: Long): Flow<Long>

    /** Get the count of songs played since a given timestamp synchronously. */
    @Query("SELECT COUNT(*) FROM PlaybackHistoryEntry WHERE timestamp >= :sinceTimestamp")
    suspend fun getPlayCountSince(sinceTimestamp: Long): Int

    /** Get the total duration of songs listened to since a given timestamp synchronously. */
    @Query(
        "SELECT COALESCE(SUM(durationPlayedMs), 0) FROM PlaybackHistoryEntry WHERE timestamp >= :sinceTimestamp"
    )
    suspend fun getTotalDurationSince(sinceTimestamp: Long): Long

    /** Delete all playback history entries. */
    @Query("DELETE FROM PlaybackHistoryEntry") suspend fun nukeHistory()
}
