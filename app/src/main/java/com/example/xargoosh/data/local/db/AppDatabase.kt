package com.example.xargoosh.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.xargoosh.data.local.entities.PlaylistEntity
import com.example.xargoosh.data.local.entities.PlaylistTrackEntity
import com.example.xargoosh.data.local.entities.TrackEntity
import com.example.xargoosh.data.local.entities.FolderEntity
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(
    entities = [TrackEntity::class, PlaylistEntity::class, PlaylistTrackEntity::class, FolderEntity::class, com.example.xargoosh.data.local.entities.LyricsEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun folderDao(): FolderDao
    abstract fun lyricsDao(): LyricsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `lyrics` (`trackUri` TEXT NOT NULL, `syncedLyrics` TEXT, `plainLyrics` TEXT, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`trackUri`))")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracks ADD COLUMN replayGainTrackDb REAL")
                db.execSQL("ALTER TABLE tracks ADD COLUMN replayGainTrackPeak REAL")
                db.execSQL("ALTER TABLE tracks ADD COLUMN replayGainAlbumDb REAL")
                db.execSQL("ALTER TABLE tracks ADD COLUMN replayGainAlbumPeak REAL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tracks ADD COLUMN filePath TEXT")
                database.execSQL("CREATE TABLE IF NOT EXISTS `folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `uriString` TEXT NOT NULL, `dateAdded` INTEGER NOT NULL)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xargoosh_database"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
