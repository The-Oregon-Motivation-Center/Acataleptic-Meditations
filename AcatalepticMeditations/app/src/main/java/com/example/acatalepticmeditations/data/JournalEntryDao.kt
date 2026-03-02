package com.example.acatalepticmeditations.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries WHERE date = :date")
    fun getEntriesForDate(date: LocalDate): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE text LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchEntries(query: String): Flow<List<JournalEntry>>

    @Insert
    suspend fun insert(entry: JournalEntry)

    @Update
    suspend fun update(entry: JournalEntry)

    @Delete
    suspend fun delete(entry: JournalEntry)
}
