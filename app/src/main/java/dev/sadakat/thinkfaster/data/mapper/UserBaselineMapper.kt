package dev.sadakat.thinkfaster.data.mapper

import dev.sadakat.thinkfaster.data.local.database.entities.UserBaselineEntity
import dev.sadakat.thinkfaster.domain.model.UserBaseline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UserBaselineMapper - Maps between entity and domain models
 * First-Week Retention Feature - Phase 1.5: Mapper
 *
 * Provides bidirectional mapping for UserBaseline
 */

/**
 * Convert UserBaselineEntity to UserBaseline (domain)
 */
fun UserBaselineEntity.toDomain(): UserBaseline {
    return UserBaseline(
        firstWeekStartDate = firstWeekStartDate,
        firstWeekEndDate = firstWeekEndDate,
        averageDailyMinutes = averageDailyMinutes,
        facebookAverageMinutes = facebookAverageMinutes,
        instagramAverageMinutes = instagramAverageMinutes
    )
}

/**
 * Convert UserBaseline (domain) to UserBaselineEntity
 */
fun UserBaseline.toEntity(): UserBaselineEntity {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = dateFormat.format(Date())

    val totalUsageMinutes = averageDailyMinutes * 7

    return UserBaselineEntity(
        id = 1,  // Always 1 (single row)
        firstWeekStartDate = firstWeekStartDate,
        firstWeekEndDate = firstWeekEndDate,
        totalUsageMinutes = totalUsageMinutes,
        averageDailyMinutes = averageDailyMinutes,
        facebookAverageMinutes = facebookAverageMinutes,
        instagramAverageMinutes = instagramAverageMinutes,
        calculatedDate = today,
        timestamp = System.currentTimeMillis()
    )
}
