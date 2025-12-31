package dev.sadakat.thinkfaster.analytics

import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import dev.sadakat.thinkfaster.domain.intervention.FrictionLevel
import dev.sadakat.thinkfaster.domain.repository.GoalRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository

/**
 * Manages user properties for Firebase Analytics segmentation
 * Properties are updated periodically to reflect user behavior changes
 */
class UserPropertiesManager(
    private val firebaseReporter: FirebaseAnalyticsReporter,
    private val usageRepository: UsageRepository,
    private val goalRepository: GoalRepository,
    private val interventionPreferences: InterventionPreferences
) {
    /**
     * Update all user properties
     * Call this on app launch and after significant user actions
     */
    suspend fun updateUserProperties() {
        val properties = mutableMapOf<String, String>()

        // Days since install (tenure)
        val daysSinceInstall = interventionPreferences.getDaysSinceInstall()
        properties["days_since_install"] = when {
            daysSinceInstall <= 1 -> "day_1"
            daysSinceInstall <= 3 -> "days_2_3"
            daysSinceInstall <= 7 -> "days_4_7"
            daysSinceInstall <= 14 -> "week_2"
            daysSinceInstall <= 30 -> "weeks_3_4"
            else -> "month_plus"
        }

        // Usage tier (based on last 7 days average)
        try {
            val weeklyAverage = usageRepository.getWeeklyAverageForApp("com.facebook.katana")
            val weeklyAverageMinutes = weeklyAverage / 60000 // ms to minutes
            properties["usage_tier"] = when {
                weeklyAverageMinutes < 30 -> "light"
                weeklyAverageMinutes < 90 -> "medium"
                else -> "heavy"
            }
        } catch (e: Exception) {
            properties["usage_tier"] = "unknown"
        }

        // Goal achievement rate (last 7 days)
        val goalAchievementRate = calculateGoalAchievementRate()
        properties["goal_achiever"] = when {
            goalAchievementRate >= 0.8f -> "high"
            goalAchievementRate >= 0.5f -> "medium"
            goalAchievementRate > 0f -> "low"
            else -> "no_goal"
        }

        // Current streak bucket
        try {
            val currentStreak = usageRepository.getCurrentStreak()
            properties["streak_status"] = when {
                currentStreak == 0 -> "no_streak"
                currentStreak <= 3 -> "beginner"
                currentStreak <= 7 -> "building"
                currentStreak <= 30 -> "committed"
                else -> "champion"
            }
        } catch (e: Exception) {
            properties["streak_status"] = "no_streak"
        }

        // Onboarding status
        properties["onboarding_completed"] = if (interventionPreferences.getInstallDate() > 0) "yes" else "no"

        // Friction level preference
        try {
            val frictionLevel = usageRepository.getEffectiveFrictionLevel()
            properties["friction_level"] = frictionLevel.name.lowercase()
        } catch (e: Exception) {
            properties["friction_level"] = "unknown"
        }

        // Feature adoption
        val hasGoals = try {
            goalRepository.getGoalByApp("com.facebook.katana") != null ||
                    goalRepository.getGoalByApp("com.instagram.android") != null
        } catch (e: Exception) {
            false
        }
        properties["has_goals"] = if (hasGoals) "yes" else "no"

        val workingModeUsed = interventionPreferences.isWorkingModeEnabled()
        properties["working_mode_user"] = if (workingModeUsed) "yes" else "no"

        // Set all properties
        firebaseReporter.setUserProperties(properties)
    }

    private suspend fun calculateGoalAchievementRate(): Float {
        return try {
            val goal = goalRepository.getGoalByApp("com.facebook.katana") ?: return 0f
            val goalMinutes = goal.dailyLimitMinutes

            // Check last 7 days
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val calendar = java.util.Calendar.getInstance()

            val last7Days = (0 until 7).count { daysAgo ->
                try {
                    calendar.time = java.util.Date()
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
                    val date = dateFormat.format(calendar.time)

                    val sessions = usageRepository.getSessionsByAppInRange("com.facebook.katana", date, date)
                    val usage = sessions.sumOf { it.duration }
                    val usageMinutes = usage / 60000
                    usageMinutes <= goalMinutes
                } catch (e: Exception) {
                    false
                }
            }

            last7Days / 7f
        } catch (e: Exception) {
            0f
        }
    }
}
