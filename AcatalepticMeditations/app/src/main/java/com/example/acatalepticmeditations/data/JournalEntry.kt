package com.example.acatalepticmeditations.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: LocalDate,
    val text: String,
    val imageUri: String? = null,
    val documentUri: String? = null,
    val documentName: String? = null
)
