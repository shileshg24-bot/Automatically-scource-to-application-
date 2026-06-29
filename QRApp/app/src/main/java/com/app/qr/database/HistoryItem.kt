package com.app.qr.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qr_history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStealth: Boolean = false,
    val size: Int = 512,
    val fgColor: Int = -16777216, // Black
    val bgColor: Int = -1 // White
)
