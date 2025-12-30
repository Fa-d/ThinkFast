package dev.sadakat.thinkfaster.data.seed.util

import dev.sadakat.thinkfaster.data.seed.config.TimeDistribution
import java.util.Calendar
import kotlin.random.Random

/**
 * Utility functions for time distribution and weighted random selection.
 */
object TimeDistributionHelper {

    /**
     * Samples an hour of day (0-23) based on the given time distribution.
     */
    fun sampleHourOfDay(distribution: TimeDistribution, random: Random = Random.Default): Int {
        val value = random.nextDouble()
        var cumulative = 0.0

        // Morning: 6am-10am (hours 6-9)
        cumulative += distribution.morning
        if (value < cumulative) {
            return random.nextInt(6, 10)
        }

        // Midday: 10am-3pm (hours 10-14)
        cumulative += distribution.midday
        if (value < cumulative) {
            return random.nextInt(10, 15)
        }

        // Evening: 3pm-8pm (hours 15-19)
        cumulative += distribution.evening
        if (value < cumulative) {
            return random.nextInt(15, 20)
        }

        // Late night: 8pm-12am (hours 20-23)
        cumulative += distribution.lateNight
        if (value < cumulative) {
            return random.nextInt(20, 24)
        }

        // Very late: 12am-6am (hours 0-5)
        return random.nextInt(0, 6)
    }

    /**
     * Weighted random selection from a list of items.
     * @param items List of items to choose from
     * @param weights Weights for each item (must sum to 1.0)
     * @return Selected item
     */
    fun <T> weightedRandom(items: List<T>, weights: List<Double>, random: Random = Random.Default): T {
        require(items.size == weights.size) { "Items and weights must have same size" }
        require(weights.sum() in 0.99..1.01) { "Weights must sum to 1.0" }

        val value = random.nextDouble()
        var cumulative = 0.0

        for (i in items.indices) {
            cumulative += weights[i]
            if (value < cumulative) {
                return items[i]
            }
        }

        return items.last()
    }

    /**
     * Checks if a given date is a weekend (Saturday or Sunday).
     */
    fun isWeekend(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }

    /**
     * Checks if a given hour is considered late night (10pm-6am).
     */
    fun isLateNight(hourOfDay: Int): Boolean {
        return hourOfDay >= 22 || hourOfDay <= 5
    }

    /**
     * Gets the day of week (1=Sunday, 7=Saturday) for a timestamp.
     */
    fun getDayOfWeek(timestamp: Long): Int {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        return calendar.get(Calendar.DAY_OF_WEEK)
    }

    /**
     * Gets the hour of day (0-23) for a timestamp.
     */
    fun getHourOfDay(timestamp: Long): Int {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    /**
     * Adds jitter to a value within a percentage range.
     * @param value Base value
     * @param jitterPercent Percentage of jitter (e.g., 0.2 = ±20%)
     * @return Value with jitter applied
     */
    fun addJitter(value: Int, jitterPercent: Double = 0.2, random: Random = Random.Default): Int {
        val jitterAmount = (value * jitterPercent).toInt()
        val adjustment = random.nextInt(-jitterAmount, jitterAmount + 1)
        return (value + adjustment).coerceAtLeast(1)
    }

    /**
     * Adds jitter to a long value within a percentage range.
     */
    fun addJitter(value: Long, jitterPercent: Double = 0.2, random: Random = Random.Default): Long {
        val jitterAmount = (value * jitterPercent).toLong()
        val adjustment = random.nextLong(-jitterAmount, jitterAmount + 1)
        return (value + adjustment).coerceAtLeast(1)
    }

    /**
     * Converts date string (YYYY-MM-DD) to timestamp at specified hour.
     */
    fun dateToTimestamp(dateString: String, hourOfDay: Int, minuteOfHour: Int = 0): Long {
        val parts = dateString.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt() - 1  // Calendar months are 0-indexed
        val day = parts[2].toInt()

        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minuteOfHour)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Formats timestamp as YYYY-MM-DD date string.
     */
    fun timestampToDateString(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    /**
     * Gets a timestamp N days ago from now.
     */
    fun getDaysAgo(days: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -days)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Gets date string for N days ago from today.
     */
    fun getDateStringDaysAgo(days: Int): String {
        return timestampToDateString(getDaysAgo(days))
    }
}
