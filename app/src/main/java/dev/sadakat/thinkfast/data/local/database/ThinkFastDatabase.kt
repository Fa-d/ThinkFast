package dev.sadakat.thinkfast.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.sadakat.thinkfast.data.local.database.dao.DailyStatsDao
import dev.sadakat.thinkfast.data.local.database.dao.GoalDao
import dev.sadakat.thinkfast.data.local.database.dao.UsageEventDao
import dev.sadakat.thinkfast.data.local.database.dao.UsageSessionDao
import dev.sadakat.thinkfast.data.local.database.entities.DailyStatsEntity
import dev.sadakat.thinkfast.data.local.database.entities.GoalEntity
import dev.sadakat.thinkfast.data.local.database.entities.UsageEventEntity
import dev.sadakat.thinkfast.data.local.database.entities.UsageSessionEntity

@Database(
    entities = [
        UsageSessionEntity::class,
        UsageEventEntity::class,
        DailyStatsEntity::class,
        GoalEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ThinkFastDatabase : RoomDatabase() {
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun usageEventDao(): UsageEventDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun goalDao(): GoalDao
}
