package dev.sadakat.thinkfast.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.sadakat.thinkfast.MainActivity
import dev.sadakat.thinkfast.R

/**
 * Helper for managing notifications
 * Phase 1.5: Quick Wins - Achievement notifications
 */
object NotificationHelper {

    private const val CHANNEL_ACHIEVEMENTS = "achievements_channel"
    private const val CHANNEL_STREAKS = "streaks_channel"

    private const val NOTIFICATION_ID_DAILY_ACHIEVEMENT = 1000
    private const val NOTIFICATION_ID_STREAK_MILESTONE = 2000

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

            notificationManager.createNotificationChannel(achievementChannel)
            notificationManager.createNotificationChannel(streakChannel)
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
}
