package com.example.traveljournal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.traveljournal.data.model.SouvenirEntity
import com.example.traveljournal.data.model.VoyageEntity

@Database(entities = [VoyageEntity::class, SouvenirEntity::class], version = 2, exportSchema = true)
abstract class TravelJournalDatabase : RoomDatabase() {
    abstract fun voyageDao(): VoyageDao
    abstract fun souvenirDao(): SouvenirDao

    companion object {
        @Volatile
        private var INSTANCE: TravelJournalDatabase? = null

        @Suppress("DEPRECATION")
        fun getInstance(context: Context): TravelJournalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TravelJournalDatabase::class.java,
                    "travel_journal.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE `voyages_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `thumbnailPath` TEXT
                    )""".trimIndent()
                )
                database.execSQL(
                    """INSERT INTO `voyages_new` (`id`, `title`, `description`, `createdAt`, `thumbnailPath`)
                        SELECT `id`, `title`, NULL, `createdAt`, `thumbnailPath` FROM `voyages`""".trimIndent()
                )
                database.execSQL("DROP TABLE `voyages`")
                database.execSQL("ALTER TABLE `voyages_new` RENAME TO `voyages`")

                database.execSQL(
                    """CREATE TABLE `souvenirs_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `mediaPath` TEXT NOT NULL,
                        `mediaType` TEXT NOT NULL,
                        `title` TEXT,
                        `description` TEXT,
                        `voyageId` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )""".trimIndent()
                )
                database.execSQL(
                    """INSERT INTO `souvenirs_new` (`id`, `mediaPath`, `mediaType`, `title`, `description`, `voyageId`, `createdAt`)
                        SELECT `id`, `imagePath`, 'PHOTO', NULL, `description`, `voyageId`, `createdAt` FROM `souvenirs`""".trimIndent()
                )
                database.execSQL("DROP TABLE `souvenirs`")
                database.execSQL("ALTER TABLE `souvenirs_new` RENAME TO `souvenirs`")
            }
        }
    }
}
