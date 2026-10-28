package com.example.vista.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.vista.data.local.entity.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    /**
     * Titles worth offering to resume.
     *
     * Three filters, each earning its place:
     *
     *  - `isCompleted = 0`: a finished title has nothing to resume.
     *  - at least 1% in: the player saves progress on open, so a title the viewer tapped once and
     *    immediately backed out of would otherwise sit in this row forever as a 0% tile. This is the
     *    single biggest reason the row used to look broken.
     *  - `duration > 0`: a title with no known duration has no meaningful fraction and no meaningful
     *    time remaining, so both labels on the card would read as 0.
     *
     * Ordered by recency so the title someone abandoned an hour ago is first, not the one from last
     * month. The limit bounds the row; the horizontal list shows a dozen at a time anyway.
     */
    @Query(
        """
        SELECT * FROM playback_history
        WHERE ownerId = :owner AND isCompleted = 0
          AND duration > 0
          AND CAST(currentPosition AS REAL) / duration >= 0.01
        ORDER BY lastPlayedTimestamp DESC
        LIMIT 20
        """
    )
    fun getContinueWatching(owner: String): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE ownerId = :owner ORDER BY lastPlayedTimestamp DESC")
    fun getAllHistory(owner: String): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE mediaId = :mediaId AND mediaType = :mediaType AND ownerId = :owner")
    suspend fun getByMediaIdAndType(mediaId: String, mediaType: String, owner: String): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE mediaId = :mediaId AND mediaType = :mediaType AND ownerId = :owner")
    suspend fun delete(mediaId: String, mediaType: String, owner: String)

    @Query("DELETE FROM playback_history WHERE ownerId = :owner")
    suspend fun deleteAll(owner: String)

    /**
     * Drops finished titles only, leaving anything mid-playback alone.
     *
     * Separate from [deleteAll] because "clear what I've finished" is the action people actually
     * want; wiping in-progress titles too throws away resume positions for shows they are halfway
     * through, which reads as the app losing their data.
     */
    @Query("DELETE FROM playback_history WHERE isCompleted = 1 AND ownerId = :owner")
    suspend fun deleteCompleted(owner: String)
}
