package dev.sadakat.thinkfaster.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usage_sessions",
    indices = [
        Index(value = ["date", "targetApp"]),
        Index(value = ["startTimestamp"]),
        Index(value = ["targetApp"])
    ]
)
data class UsageSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetApp: String,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val duration: Long,
    val wasInterrupted: Boolean = false,
    val interruptionType: String? = null,
    val date: String  // YYYY-MM-DD for easy grouping
)
