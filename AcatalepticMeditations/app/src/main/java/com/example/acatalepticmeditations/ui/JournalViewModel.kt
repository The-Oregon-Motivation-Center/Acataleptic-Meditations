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

    fun getAllEntries(descending: Boolean = true): Flow<List<JournalEntry>> {
        return if (descending) dao.getAllEntriesDescending() else dao.getAllEntriesAscending()
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

    fun updateScores(date: LocalDate, sessionScore: Int) {
        viewModelScope.launch {
            val currentDaily = dao.getScoreForDate(date).first() ?: DailyScore(date, 0, 0)
            val newHigh = maxOf(currentDaily.highScore, sessionScore)
            val newTotal = currentDaily.totalScore + 1
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
