package com.example.acatalepticmeditations.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.acatalepticmeditations.data.DailyScore
import com.example.acatalepticmeditations.data.JournalEntry
import com.example.acatalepticmeditations.data.JournalEntryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class JournalViewModel(private val dao: JournalEntryDao) : ViewModel() {

    fun getEntriesForDate(date: LocalDate): Flow<List<JournalEntry>> {
        return dao.getEntriesForDate(date)
    }

    fun searchEntries(query: String): Flow<List<JournalEntry>> {
        return dao.searchEntries(query)
    }

    fun addJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            dao.insert(entry)
        }
    }

    fun updateJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            dao.update(entry)
        }
    }

    fun deleteJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            dao.delete(entry)
        }
    }

    // Ripple Game Score methods
    fun getScoreForDate(date: LocalDate): Flow<DailyScore?> {
        return dao.getScoreForDate(date)
    }

    fun incrementScore(date: LocalDate) {
        viewModelScope.launch {
            val currentScore = dao.getScoreForDate(date).first() ?: DailyScore(date, 0, 0)
            val newTotal = currentScore.totalScore + 1
            // High score is the max of current high score and current game session if we were tracking it, 
            // but the prompt says "high score" and "total". 
            // Let's assume high score is the most taps in a single day or session? 
            // Actually, usually high score is best session. 
            // But let's just keep it as "highest total ever reached in a day" for now if not specified.
            // Wait, the previous logic was: if (score > high) updateHighScore.
            // Let's stick to updating high score when current session score > daily high score.
        }
    }

    fun updateScores(date: LocalDate, sessionScore: Int) {
        viewModelScope.launch {
            val currentDaily = dao.getScoreForDate(date).first() ?: DailyScore(date, 0, 0)
            val newHigh = maxOf(currentDaily.highScore, sessionScore)
            val newTotal = currentDaily.totalScore + 1 // This is called every tap
            dao.insertScore(DailyScore(date, newHigh, newTotal))
        }
    }

    fun getAllScores(): Flow<List<DailyScore>> {
        return dao.getAllScores()
    }
}

class JournalViewModelFactory(private val dao: JournalEntryDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JournalViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
