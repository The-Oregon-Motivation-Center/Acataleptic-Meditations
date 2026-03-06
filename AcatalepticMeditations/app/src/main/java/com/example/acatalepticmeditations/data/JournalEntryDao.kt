package com.acataleptic.meditations.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries WHERE date = :date")
    fun getEntriesForDate(date: LocalDate): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE text LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchEntries(query: String): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun getAllEntriesDescending(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries ORDER BY date ASC")
    fun getAllEntriesAscending(): Flow<List<JournalEntry>>

    @Insert
    suspend fun insert(entry: JournalEntry)

    @Update
    suspend fun update(entry: JournalEntry)

    @Delete
    suspend fun delete(entry: JournalEntry)

    // Daily Score methods
    @Query("SELECT * FROM daily_scores WHERE date = :date")
    fun getScoreForDate(date: LocalDate): Flow<DailyScore?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: DailyScore)

    @Query("UPDATE daily_scores SET totalScore = totalScore + 1, highScore = CASE WHEN :sessionScore > highScore THEN :sessionScore ELSE highScore END WHERE date = :date")
    suspend fun incrementDailyScore(date: LocalDate, sessionScore: Int)

    @Query("SELECT * FROM daily_scores")
    fun getAllScores(): Flow<List<DailyScore>>
}
