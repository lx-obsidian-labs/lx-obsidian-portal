package com.example.vista.data.local.dao

import androidx.room.*
import com.example.vista.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist WHERE ownerId = :owner ORDER BY addedAt DESC")
    fun getAll(owner: String): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id AND type = :type AND ownerId = :owner)")
    suspend fun exists(id: String, type: String, owner: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE id = :id AND type = :type AND ownerId = :owner")
    suspend fun delete(id: String, type: String, owner: String)

    @Query("DELETE FROM watchlist WHERE ownerId = :owner")
    suspend fun deleteAll(owner: String)
}
