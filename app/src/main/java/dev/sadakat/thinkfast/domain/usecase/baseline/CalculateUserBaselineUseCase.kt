package dev.sadakat.thinkfast.domain.usecase.baseline

import dev.sadakat.thinkfast.domain.model.UserBaseline
import dev.sadakat.thinkfast.domain.repository.GoalRepository
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import dev.sadakat.thinkfast.domain.repository.UserBaselineRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * CalculateUserBaselineUseCase - Calculates user's baseline from first 7 days
 * First-Week Retention Feature - Phase 2: Business Logic
 *
 * Calculates average usage from the first week after setting goals.
 * Called by AchievementWorker on Day 7, or HomeViewModel on demand.
 */
class CalculateUserBaselineUseCase(
    private val usageRepository: UsageRepository,
    private val goalRepository: GoalRepository,
    private val baselineRepository: UserBaselineRepository
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend operator fun invoke(): UserBaseline? {
        // 1. Get earliest goal start date (user's onboarding date)
        val allGoals = goalRepository.getAllGoals()
        if (allGoals.isEmpty()) return null

        val earliestGoal = allGoals.minByOrNull { it.startDate }
        val startDate = earliestGoal?.startDate ?: return null

        // 2. Calculate 7 days from start
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(startDate) ?: return null
        val endCal = calendar.clone() as Calendar
        endCal.add(Calendar.DAY_OF_YEAR, 6)
        val endDate = dateFormat.format(endCal.time)

        // 3. Check if we have 7 days of data
        val today = dateFormat.format(Date())
        if (endDate > today) return null  // Not enough data yet

        // 4. Get sessions for first week
        val facebookSessions = usageRepository.getSessionsByAppInRange(
            "com.facebook.katana", startDate, endDate
        )
        val instagramSessions = usageRepository.getSessionsByAppInRange(
            "com.instagram.android", startDate, endDate
        )

        // 5. Calculate averages (convert ms to minutes)
        val facebookMinutes = (facebookSessions.sumOf { it.duration } / 1000 / 60).toInt()
        val instagramMinutes = (instagramSessions.sumOf { it.duration } / 1000 / 60).toInt()
        val totalMinutes = facebookMinutes + instagramMinutes
        val averageDailyMinutes = totalMinutes / 7

        // 6. Create and save baseline
        val baseline = UserBaseline(
            firstWeekStartDate = startDate,
            firstWeekEndDate = endDate,
            averageDailyMinutes = averageDailyMinutes,
            facebookAverageMinutes = facebookMinutes / 7,
            instagramAverageMinutes = instagramMinutes / 7
        )

        baselineRepository.saveBaseline(baseline)
        return baseline
    }
}
