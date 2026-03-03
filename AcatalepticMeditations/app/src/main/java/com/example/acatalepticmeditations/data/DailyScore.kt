package com.example.acatalepticmeditations.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_scores")
data class DailyScore(
    @PrimaryKey
    val date: LocalDate,
    val highScore: Int,
    val totalScore: Int = 0
)
