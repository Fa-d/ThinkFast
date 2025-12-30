package dev.sadakat.thinkfaster.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.sadakat.thinkfaster.MainActivity
import dev.sadakat.thinkfaster.R

/**
 * Helper for managing notifications
 * Phase 1.5: Quick Wins - Achievement notifications
 */
object NotificationHelper {

    private const val CHANNEL_ACHIEVEMENTS = "achievements_channel"
    private const val CHANNEL_STREAKS = "streaks_channel"
    private const val CHANNEL_STREAK_RECOVERY = "streak_recovery_channel"
    private const val CHANNEL_MOTIVATIONAL = "motivational_channel"

    private const val NOTIFICATION_ID_DAILY_ACHIEVEMENT = 1000
    private const val NOTIFICATION_ID_STREAK_MILESTONE = 2000
    private const val NOTIFICATION_ID_STREAK_BROKEN = 3000
    private const val NOTIFICATION_ID_RECOVERY_PROGRESS = 4000
    private const val NOTIFICATION_ID_QUEST_DAY = 5000
    private const val NOTIFICATION_ID_QUEST_COMPLETE = 5010
    private const val NOTIFICATION_ID_MORNING_INTENTION = 6000
    private const val NOTIFICATION_ID_EVENING_REVIEW = 6001
    private const val NOTIFICATION_ID_STREAK_WARNING = 6002

    /**
     * Create notification channels (must be called on app start)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Achievement notifications channel
            val achievementChannel = NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                "Daily Achievements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when you meet your daily goals"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 30, 50, 60) // Success pattern
            }

            // Streak milestone notifications channel
            val streakChannel = NotificationChannel(
                CHANNEL_STREAKS,
                "Streak Milestones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Special notifications for streak milestones (7, 14, 30 days)"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 40, 50, 50, 50, 60, 50, 80) // Celebration pattern
            }

            // Streak recovery notifications channel
            val recoveryChannel = NotificationChannel(
                CHANNEL_STREAK_RECOVERY,
                "Streak Recovery",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when streaks break and recovery progress"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 40, 60, 40)
            }

            // Motivational notifications channel (Push Notification Strategy)
            val motivationalChannel = NotificationChannel(
                CHANNEL_MOTIVATIONAL,
                "Motivational Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily motivation and streak reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 30, 50, 60) // Success pattern
            }

            notificationManager.createNotificationChannel(achievementChannel)
            notificationManager.createNotificationChannel(streakChannel)
            notificationManager.createNotificationChannel(recoveryChannel)
            notificationManager.createNotificationChannel(motivationalChannel)
        }
    }

    /**
     * Show notification for meeting daily goal
     */
    fun showDailyAchievementNotification(
        context: Context,
        usageMinutes: Int,
        goalMinutes: Int,
        currentStreak: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Goal Achieved! 🎯")
            .setContentText("You stayed under $goalMinutes minutes today! Used: $usageMinutes min")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Great job! You used $usageMinutes minutes today, staying under your $goalMinutes minute goal.\n\n🔥 Current streak: $currentStreak ${if (currentStreak == 1) "day" else "days"}!")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_DAILY_ACHIEVEMENT, notification)
    }

    /**
     * Show notification for first-time achievement (first day meeting goal)
     */
    fun showFirstTimeAchievementNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("First Goal Achieved! 🌟")
            .setContentText("Congratulations on your first day meeting your goal!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Amazing work! This is just the beginning of building better social media habits. Keep it up!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_DAILY_ACHIEVEMENT + 1, notification)
    }

    /**
     * Show notification for streak milestones (3, 7, 14, 30 days)
     */
    fun showStreakMilestoneNotification(
        context: Context,
        streakDays: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Determine emoji and message based on milestone
        val (emoji, title, message) = when (streakDays) {
            3 -> Triple("🔥", "3-Day Streak!", "You're on fire! Three days in a row!")
            7 -> Triple("⭐", "Week-Long Streak!", "Incredible! A full week of meeting your goals!")
            14 -> Triple("💎", "2-Week Streak!", "You're a diamond! Two weeks of dedication!")
            30 -> Triple("👑", "30-Day Streak!", "You're royalty! A full month of success!")
            else -> Triple("🎉", "$streakDays-Day Streak!", "$streakDays days in a row! Amazing!")
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_STREAKS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$emoji $title")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$message\n\nYou've been consistently meeting your social media goals for $streakDays days. This is what building healthy habits looks like! 💪")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_STREAK_MILESTONE + streakDays, notification)
    }

    /**
     * Check if this is a milestone streak worth celebrating
     */
    fun isStreakMilestone(streak: Int): Boolean {
        return streak in listOf(1, 3, 7, 14, 30) || (streak > 30 && streak % 30 == 0)
    }

    /**
     * Show notification when a streak breaks
     * Broken Streak Recovery feature
     */
    fun showStreakBrokenNotification(
        context: Context,
        targetApp: String,
        previousStreak: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val appName = when (targetApp) {
            "com.facebook.katana" -> "Facebook"
            "com.instagram.android" -> "Instagram"
            else -> "App"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_STREAK_RECOVERY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("💔 Streak Ended")
            .setContentText("Your $previousStreak-day $appName streak ended, but you can bounce back!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your $previousStreak-day $appName streak was amazing! Don't give up—you're just 1 day away from starting your comeback. We'll celebrate your recovery even more! 🔄")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_STREAK_BROKEN + targetApp.hashCode(), notification)
    }

    /**
     * Show notification for recovery milestone progress
     * Broken Streak Recovery feature
     */
    fun showRecoveryMilestoneNotification(
        context: Context,
        targetApp: String,
        daysRecovered: Int,
        targetDays: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val appName = when (targetApp) {
            "com.facebook.katana" -> "Facebook"
            "com.instagram.android" -> "Instagram"
            else -> "App"
        }

        val remaining = targetDays - daysRecovered

        val notification = NotificationCompat.Builder(context, CHANNEL_STREAK_RECOVERY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔄 Recovery Progress: Day $daysRecovered")
            .setContentText("Just $remaining more ${if (remaining == 1) "day" else "days"} until you're back on track!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Great job! You've stayed on track for $daysRecovered ${if (daysRecovered == 1) "day" else "days"}. Keep going—only $remaining more ${if (remaining == 1) "day" else "days"} until you're back on track! 💪")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_RECOVERY_PROGRESS + targetApp.hashCode(), notification)
    }

    /**
     * Show notification when recovery is complete
     * Broken Streak Recovery feature
     */
    fun showRecoveryCompleteNotification(
        context: Context,
        targetApp: String,
        previousStreak: Int,
        daysToRecover: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val appName = when (targetApp) {
            "com.facebook.katana" -> "Facebook"
            "com.instagram.android" -> "Instagram"
            else -> "App"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_STREAK_RECOVERY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎉 You're Back on Track!")
            .setContentText("Amazing comeback! You recovered in just $daysToRecover ${if (daysToRecover == 1) "day" else "days"}!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Incredible resilience! You broke your $previousStreak-day $appName streak but got back on track in just $daysToRecover ${if (daysToRecover == 1) "day" else "days"}. This shows real commitment to your goals! 💪🎉")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_RECOVERY_PROGRESS + targetApp.hashCode(), notification)
    }

    /**
     * Show notification for quest day completion (Days 1, 2)
     * First-Week Retention feature
     */
    fun showQuestDayNotification(
        context: Context,
        day: Int,
        emoji: String,
        message: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$emoji Day $day Complete!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_QUEST_DAY + day, notification)
    }

    /**
     * Show notification when 7-day quest is complete
     * First-Week Retention feature
     */
    fun showQuestCompleteNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎉 7-Day Quest Complete!")
            .setContentText("Amazing! You've completed your first week. You're building lasting habits!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Incredible achievement! You've successfully completed the 7-day onboarding quest. You're building lasting habits and taking control of your social media usage. Keep it up! 🌟")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_QUEST_COMPLETE, notification)
    }

    /**
     * Show morning intention notification
     * Push Notification Strategy: Daily motivation to set intentions
     */
    fun showMorningIntentionNotification(
        context: Context,
        title: String,
        message: String,
        currentStreak: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MOTIVATIONAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$message\n\n🔥 Current streak: $currentStreak ${if (currentStreak == 1) "day" else "days"}")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_MORNING_INTENTION, notification)
    }

    /**
     * Show evening review notification
     * Push Notification Strategy: Daily usage summary with weekly progress
     */
    fun showEveningReviewNotification(
        context: Context,
        title: String,
        message: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MOTIVATIONAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_EVENING_REVIEW, notification)
    }

    /**
     * Show streak warning notification
     * Push Notification Strategy: Urgent warnings when streak is at risk
     */
    fun showStreakWarningNotification(
        context: Context,
        title: String,
        message: String,
        streakDays: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_STREAKS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$message\n\nDon't lose your progress! Open the app to check your usage.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_STREAK_WARNING, notification)
    }
}
