package dev.sadakat.thinkfaster.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usage_events",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["timestamp"]),
        Index(value = ["eventType"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = UsageSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UsageEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val eventType: String,
    val timestamp: Long,
    val metadata: String? = null,

    // Sync metadata for multi-device sync
    @ColumnInfo(name = "user_id", defaultValue = "NULL")
    val userId: String? = null,

    @ColumnInfo(name = "sync_status", defaultValue = "PENDING")
    val syncStatus: String = "PENDING",

    @ColumnInfo(name = "last_modified", defaultValue = "0")
    val lastModified: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "cloud_id", defaultValue = "NULL")
    val cloudId: String? = null
)
