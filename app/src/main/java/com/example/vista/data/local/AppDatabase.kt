package com.example.vista.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.vista.data.local.dao.*
import com.example.vista.data.local.entity.*

@Database(
    entities = [
        WatchlistEntity::class,
        PlaybackHistoryEntity::class,
        RecentSearchEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun recentSearchDao(): RecentSearchDao

    companion object {
        /** Preserve every row; scope new data by profile and normalize legacy type casing. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE watchlist_v6 (id TEXT NOT NULL, title TEXT NOT NULL, posterUrl TEXT, backdropUrl TEXT, rating REAL, year INTEGER, type TEXT NOT NULL, addedAt INTEGER NOT NULL, ownerId TEXT NOT NULL, PRIMARY KEY(id,type,ownerId))")
                db.execSQL("INSERT OR REPLACE INTO watchlist_v6 SELECT id,title,posterUrl,backdropUrl,rating,year,LOWER(type),addedAt,'' FROM watchlist ORDER BY addedAt ASC")
                db.execSQL("DROP TABLE watchlist")
                db.execSQL("ALTER TABLE watchlist_v6 RENAME TO watchlist")
                db.execSQL("CREATE TABLE playback_history_v6 (mediaId TEXT NOT NULL, mediaType TEXT NOT NULL, title TEXT NOT NULL, posterUrl TEXT, rating REAL, currentPosition INTEGER NOT NULL, duration INTEGER NOT NULL, lastPlayedTimestamp INTEGER NOT NULL, seasonNumber INTEGER, episodeNumber INTEGER, isCompleted INTEGER NOT NULL, ownerId TEXT NOT NULL, PRIMARY KEY(mediaId,mediaType,ownerId))")
                db.execSQL("INSERT OR REPLACE INTO playback_history_v6 SELECT mediaId,LOWER(mediaType),title,posterUrl,rating,currentPosition,duration,lastPlayedTimestamp,seasonNumber,episodeNumber,isCompleted,'' FROM playback_history ORDER BY lastPlayedTimestamp ASC")
                db.execSQL("DROP TABLE playback_history")
                db.execSQL("ALTER TABLE playback_history_v6 RENAME TO playback_history")
            }
        }
        /**
         * v1 -> v2 ships with the same schema (1.json and 2.json are byte-identical), so no SQL
         * is needed; without this entry legacy installs would crash on upgrade before v3.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        /**
         * v2 -> v3 rebuilds `watchlist` and `playback_history` with composite primary keys
         * (id, type) so a movie and a series that happen to share a provider id can no longer
         * overwrite each other. Rows are carried over unchanged.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `watchlist_new` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `posterUrl` TEXT,
                        `backdropUrl` TEXT,
                        `rating` REAL,
                        `year` INTEGER,
                        `type` TEXT NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`, `type`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `watchlist_new` (`id`, `title`, `posterUrl`, `backdropUrl`, `rating`, `year`, `type`, `addedAt`)
                    SELECT `id`, `title`, `posterUrl`, `backdropUrl`, `rating`, `year`, `type`, `addedAt` FROM `watchlist`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `watchlist`")
                db.execSQL("ALTER TABLE `watchlist_new` RENAME TO `watchlist`")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `playback_history_new` (
                        `mediaId` TEXT NOT NULL,
                        `mediaType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `posterUrl` TEXT,
                        `rating` REAL,
                        `currentPosition` INTEGER NOT NULL,
                        `duration` INTEGER NOT NULL,
                        `lastPlayedTimestamp` INTEGER NOT NULL,
                        `seasonNumber` INTEGER,
                        `episodeNumber` INTEGER,
                        `isCompleted` INTEGER NOT NULL,
                        PRIMARY KEY(`mediaId`, `mediaType`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `playback_history_new`
                        (`mediaId`, `mediaType`, `title`, `posterUrl`, `rating`, `currentPosition`, `duration`, `lastPlayedTimestamp`, `seasonNumber`, `episodeNumber`, `isCompleted`)
                    SELECT `mediaId`, `mediaType`, `title`, `posterUrl`, `rating`, `currentPosition`, `duration`, `lastPlayedTimestamp`, `seasonNumber`, `episodeNumber`, `isCompleted` FROM `playback_history`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `playback_history`")
                db.execSQL("ALTER TABLE `playback_history_new` RENAME TO `playback_history`")
            }
        }

        /**
         * v3 -> v4 adds the offline downloads table. It is purely additive: no existing table is
         * touched, so no user data is rewritten and there is nothing to copy across.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `downloads` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `mediaId` TEXT NOT NULL,
                        `mediaType` TEXT NOT NULL,
                        `seasonNumber` INTEGER NOT NULL,
                        `episodeNumber` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `posterUrl` TEXT,
                        `episodeTitle` TEXT,
                        `filePath` TEXT,
                        `containerFormat` TEXT,
                        `bytesDownloaded` INTEGER NOT NULL DEFAULT 0,
                        `totalBytes` INTEGER NOT NULL DEFAULT 0,
                        `sourceName` TEXT,
                        `sourceUrl` TEXT,
                        `sourceHeadersJson` TEXT,
                        `sourceExpiresAt` INTEGER,
                        `status` TEXT NOT NULL DEFAULT 'queued',
                        `errorMessage` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `completedAt` INTEGER
                    )
                    """.trimIndent()
                )
                // Names and uniqueness here must match the DownloadEntity @Index declarations that v4 shipped.
                // Room validates the schema it exported against the live database on every open, so a
                // mismatched index is a crash, not a warning - and an index created only in this
                // migration is absent on every fresh install.
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_downloads_mediaId_mediaType_seasonNumber_episodeNumber` " +
                        "ON `downloads` (`mediaId`, `mediaType`, `seasonNumber`, `episodeNumber`)"
                )
                // Status lookups back the downloads list and the startup reconciliation sweep.
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_downloads_status` ON `downloads` (`status`)"
                )
            }
        }

        /**
         * v4 -> v5 removes offline downloads entirely.
         *
         * The feature could not be made to work: every provider the app resolved against served an
         * adaptive playlist rather than a saveable file, so no title could ever be downloaded. The
         * table and its two indexes go with it.
         *
         * A version bump is mandatory rather than cosmetic. Room stores an identity hash derived
         * from the expected schema, so removing the entity without bumping the version leaves every
         * existing install crashing on open with a hash mismatch - the table would have to stay
         * dormant forever to avoid it.
         *
         * `IF EXISTS` throughout: a fresh install creates the database straight at v5 and never runs
         * this, while an install that somehow lacks the table should still open rather than abort.
         * Watchlist and history are untouched, so no user data is lost.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS `index_downloads_status`")
                db.execSQL(
                    "DROP INDEX IF EXISTS `index_downloads_mediaId_mediaType_seasonNumber_episodeNumber`"
                )
                db.execSQL("DROP TABLE IF EXISTS `downloads`")
            }
        }
    }
}
