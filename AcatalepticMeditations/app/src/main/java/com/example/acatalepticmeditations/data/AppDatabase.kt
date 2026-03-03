package com.example.acatalepticmeditations.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [JournalEntry::class, DailyScore::class], 
    version = 5,
    exportSchema = true // Required for safe migrations
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "journal_database"
                )
                // Removed .fallbackToDestructiveMigration() to protect user data
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
