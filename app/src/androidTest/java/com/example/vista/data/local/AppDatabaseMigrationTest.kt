package com.example.vista.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java, emptyList(), FrameworkSQLiteOpenHelperFactory())
    private val migrations = arrayOf(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6)

    @Test fun everyShippedSchemaUpgradesAndPreservesResumeAndWatchlist() {
        for (version in 1..5) {
            val name = "migration-from-$version.db"
            helper.createDatabase(name, version).apply {
                execSQL("INSERT INTO watchlist (id,title,posterUrl,backdropUrl,rating,year,type,addedAt) VALUES ('42','Saved title',NULL,NULL,8.1,2020,'movie',123)")
                execSQL("INSERT INTO playback_history (mediaId,mediaType,title,posterUrl,rating,currentPosition,duration,lastPlayedTimestamp,seasonNumber,episodeNumber,isCompleted) VALUES ('42','SERIES','Episode',NULL,8.1,60000,120000,456,2,3,0)")
                close()
            }
            helper.runMigrationsAndValidate(name, 6, true, *migrations).close()
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val room = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(*migrations).build()
            val db = room.openHelper.writableDatabase // Actual Room identity validation, not createDatabase.
            db.query("SELECT title,addedAt,ownerId FROM watchlist WHERE id='42'").use {
                assertTrue(it.moveToFirst()); assertEquals("Saved title", it.getString(0)); assertEquals(123L, it.getLong(1)); assertEquals("", it.getString(2))
            }
            db.query("SELECT mediaType,currentPosition,seasonNumber,episodeNumber,ownerId FROM playback_history WHERE mediaId='42'").use {
                assertTrue(it.moveToFirst()); assertEquals("series", it.getString(0)); assertEquals(60000L, it.getLong(1)); assertEquals(2, it.getInt(2)); assertEquals(3, it.getInt(3)); assertEquals("", it.getString(4))
            }
            room.close()
        }
    }

    @Test fun profilesCanSaveSameTitleWithoutOverwritingEachOther() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val room = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val db = room.openHelper.writableDatabase
        for (owner in listOf("profile-a", "profile-b")) {
            db.execSQL("INSERT INTO watchlist (id,title,type,addedAt,ownerId) VALUES ('1','Title','movie',1,?)", arrayOf(owner))
        }
        db.execSQL("DELETE FROM watchlist WHERE ownerId='profile-a'")
        db.query("SELECT ownerId FROM watchlist").use { assertTrue(it.moveToFirst()); assertEquals("profile-b", it.getString(0)); assertFalse(it.moveToNext()) }
        room.close()
    }
}
