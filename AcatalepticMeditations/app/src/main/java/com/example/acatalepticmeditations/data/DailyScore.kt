package com.acataleptic.meditations.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_scores")
data class DailyScore(
    @PrimaryKey
    val date: LocalDate,
    val rippleHighScore: Int = 0,
    val rippleTotalScore: Int = 0,
    val tracerHighScore: Int = 0,
    val tracerTotalScore: Int = 0
)
