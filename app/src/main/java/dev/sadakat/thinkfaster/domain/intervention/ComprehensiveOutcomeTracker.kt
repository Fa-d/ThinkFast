package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.data.local.database.dao.ComprehensiveOutcomeDao
import dev.sadakat.thinkfaster.data.local.database.dao.UsageSessionDao
import dev.sadakat.thinkfaster.data.local.database.entities.ComprehensiveOutcomeEntity
import dev.sadakat.thinkfaster.data.seed.util.TimeDistributionHelper.timestampToDateString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 1: Comprehensive Outcome Tracker
 *
 * Manages the collection of intervention outcomes across multiple time windows:
 * - Proximal (immediate): Collected right after intervention
 * - Short-term (5-30 min): Collected after short delay
 * - Medium-term (same day): Collected at end of day
 * - Long-term (7-30 days): Collected after week+
 *
 * This tracker runs background jobs to progressively update outcomes as time passes.
 *
 * Usage:
 * ```
 * // Record immediate outcome
 * tracker.recordProximalOutcome(interventionId, userChoice, responseTime, depth)
 *
 * // Background job to collect pending updates
 * tracker.collectPendingShortTermOutcomes()
 * tracker.collectPendingMediumTermOutcomes()
 * tracker.collectPendingLongTermOutcomes()
 * ```
 */
@Singleton
class ComprehensiveOutcomeTracker @Inject constructor(
    private val comprehensiveOutcomeDao: ComprehensiveOutcomeDao,
    private val usageSessionDao: UsageSessionDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    /**
     * Record immediate (proximal) outcome when user responds to intervention
     * This should be called immediately after intervention is shown
     */
    suspend fun recordProximalOutcome(
        interventionId: Long,
        sessionId: Long,
        targetApp: String,
        userChoice: InterventionUserChoice,
        responseTime: Long,
        interactionDepth: InteractionDepth
    ): Long = withContext(Dispatchers.IO) {
        val entity = ComprehensiveOutcomeEntity(
            interventionId = interventionId,
            sessionId = sessionId,
            targetApp = targetApp,
            timestamp = System.currentTimeMillis(),
            immediateChoice = userChoice.name,
            responseTime = responseTime,
            interactionDepth = interactionDepth.name,
            proximalCollected = true,
            lastUpdated = System.currentTimeMillis()
        )

        comprehensiveOutcomeDao.insert(entity)
    }

    /**
     * Collect short-term outcomes (5-30 minutes after intervention)
     * Should be run periodically by a background worker
     */
    suspend fun collectPendingShortTermOutcomes(limit: Int = 50) = withContext(Dispatchers.IO) {
        val outcomes = comprehensiveOutcomeDao.getOutcomesNeedingShortTermCollection(limit = limit)

        outcomes.forEach { outcome ->
            try {
                val updated = collectShortTermOutcome(outcome)
                comprehensiveOutcomeDao.update(updated)
            } catch (e: Exception) {
                // Log error but continue processing other outcomes
                dev.sadakat.thinkfaster.util.ErrorLogger.warning(
                    message = "Failed to collect short-term outcome for intervention ${outcome.interventionId}: ${e.message}",
                    context = "ComprehensiveOutcomeTracker"
                )
            }
        }
    }

    /**
     * Collect medium-term outcomes (same day, after session ends)
     * Should be run periodically by a background worker
     */
    suspend fun collectPendingMediumTermOutcomes(limit: Int = 50) = withContext(Dispatchers.IO) {
        val outcomes = comprehensiveOutcomeDao.getOutcomesNeedingMediumTermCollection(limit = limit)

        outcomes.forEach { outcome ->
            try {
                val updated = collectMediumTermOutcome(outcome)
                comprehensiveOutcomeDao.update(updated)
            } catch (e: Exception) {
                dev.sadakat.thinkfaster.util.ErrorLogger.warning(
                    message = "Failed to collect medium-term outcome for intervention ${outcome.interventionId}: ${e.message}",
                    context = "ComprehensiveOutcomeTracker"
                )
            }
        }
    }

    /**
     * Collect long-term outcomes (7-30 days after intervention)
     * Should be run daily by a background worker
     */
    suspend fun collectPendingLongTermOutcomes(limit: Int = 50) = withContext(Dispatchers.IO) {
        val outcomes = comprehensiveOutcomeDao.getOutcomesNeedingLongTermCollection(limit = limit)

        outcomes.forEach { outcome ->
            try {
                val updated = collectLongTermOutcome(outcome)
                comprehensiveOutcomeDao.update(updated)
            } catch (e: Exception) {
                dev.sadakat.thinkfaster.util.ErrorLogger.warning(
                    message = "Failed to collect long-term outcome for intervention ${outcome.interventionId}: ${e.message}",
                    context = "ComprehensiveOutcomeTracker"
                )
            }
        }
    }

    /**
     * Calculate and cache reward score for an outcome
     * Should be called after each collection window update
     */
    suspend fun updateRewardScore(outcomeId: Long) = withContext(Dispatchers.IO) {
        val outcome = comprehensiveOutcomeDao.getById(outcomeId) ?: return@withContext

        val domainOutcome = outcome.toDomainModel()
        val rewardScore = domainOutcome.calculateReward()

        // Update with new reward score
        comprehensiveOutcomeDao.update(
            outcome.copy(
                rewardScore = rewardScore,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    // ========== PRIVATE HELPER METHODS ==========

    private suspend fun collectShortTermOutcome(
        outcome: ComprehensiveOutcomeEntity
    ): ComprehensiveOutcomeEntity {
        // Get session info
        val session = usageSessionDao.getSessionById(outcome.sessionId)

        // Check if session continued after intervention
        val sessionContinued = session != null && session.endTimestamp == null

        // Calculate session duration after intervention
        val sessionDurationAfter = if (session != null && session.endTimestamp != null) {
            val interventionTime = outcome.timestamp
            val sessionEndTime = session.endTimestamp ?: System.currentTimeMillis()
            maxOf(0, sessionEndTime - interventionTime)
        } else null

        // Check for quick reopens (within 5 minutes)
        val fiveMinutesAfter = outcome.timestamp + (5 * 60 * 1000)
        val startDate = timestampToDateString(outcome.timestamp)
        val endDate = timestampToDateString(fiveMinutesAfter)
        val subsequentSessions = usageSessionDao.getSessionsInRangeForApp(
            startDate = startDate,
            endDate = endDate,
            targetApp = outcome.targetApp
        )
        val quickReopen = subsequentSessions.size > 1  // More than just the current session

        // Count reopens in 30-minute window
        val thirtyMinutesAfter = outcome.timestamp + (30 * 60 * 1000)
        val endDate30 = timestampToDateString(thirtyMinutesAfter)
        val allSessionsIn30Min = usageSessionDao.getSessionsInRangeForApp(
            startDate = startDate,
            endDate = endDate30,
            targetApp = outcome.targetApp
        )
        val reopenCount30Min = maxOf(0, allSessionsIn30Min.size - 1)

        // TODO: Check if user switched to productive app
        // This requires tracking app categories or user-defined productive apps
        val switchedToProductiveApp = false  // Placeholder

        return outcome.copy(
            sessionContinued = sessionContinued,
            sessionDurationAfter = sessionDurationAfter,
            quickReopen = quickReopen,
            reopenCount30Min = reopenCount30Min,
            switchedToProductiveApp = switchedToProductiveApp,
            shortTermCollected = true,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private suspend fun collectMediumTermOutcome(
        outcome: ComprehensiveOutcomeEntity
    ): ComprehensiveOutcomeEntity {
        // Get all sessions for this app on the same day
        val dayStart = getDayStart(outcome.timestamp)
        val dayEnd = getDayEnd(outcome.timestamp)
        val startDate = timestampToDateString(dayStart)
        val endDate = timestampToDateString(dayEnd)

        val sessionsToday = usageSessionDao.getSessionsInRangeForApp(
            startDate = startDate,
            endDate = endDate,
            targetApp = outcome.targetApp
        )

        // Count sessions after intervention
        val additionalSessionsToday = sessionsToday.count { it.startTimestamp > outcome.timestamp }

        // Calculate total usage for the day
        val totalUsageToday = sessionsToday
            .filter { it.endTimestamp != null }
            .sumOf { (it.endTimestamp!! - it.startTimestamp) / (60 * 1000L) }  // Convert to minutes

        // TODO: Calculate usage reduction compared to typical day
        // This requires historical baseline data
        val totalUsageReductionToday: Long? = null  // Placeholder

        // TODO: Check if goal was met
        // This requires querying GoalEntity
        val goalMetToday: Boolean? = null  // Placeholder

        return outcome.copy(
            additionalSessionsToday = additionalSessionsToday,
            totalScreenTimeToday = totalUsageToday,
            totalUsageReductionToday = totalUsageReductionToday,
            goalMetToday = goalMetToday,
            mediumTermCollected = true,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private suspend fun collectLongTermOutcome(
        outcome: ComprehensiveOutcomeEntity
    ): ComprehensiveOutcomeEntity {
        // Calculate 7-day window after intervention
        val sevenDaysAfterStart = outcome.timestamp + (7 * 24 * 60 * 60 * 1000)
        val sevenDaysAfterEnd = outcome.timestamp + (14 * 24 * 60 * 60 * 1000)
        val afterStartDate = timestampToDateString(sevenDaysAfterStart)
        val afterEndDate = timestampToDateString(sevenDaysAfterEnd)

        // Get sessions in the week after intervention
        val sessionsAfter = usageSessionDao.getSessionsInRangeForApp(
            startDate = afterStartDate,
            endDate = afterEndDate,
            targetApp = outcome.targetApp
        )

        // Calculate average daily usage in next 7 days
        val totalUsageNext7Days = sessionsAfter
            .filter { it.endTimestamp != null }
            .sumOf { (it.endTimestamp!! - it.startTimestamp) / (60 * 1000L) }  // Minutes

        val avgDailyUsageNext7Days = if (sessionsAfter.isNotEmpty()) {
            totalUsageNext7Days / 7
        } else 0L

        // Compare to week before intervention
        val sevenDaysBeforeStart = outcome.timestamp - (14 * 24 * 60 * 60 * 1000)
        val sevenDaysBeforeEnd = outcome.timestamp - (7 * 24 * 60 * 60 * 1000)
        val beforeStartDate = timestampToDateString(sevenDaysBeforeStart)
        val beforeEndDate = timestampToDateString(sevenDaysBeforeEnd)

        val sessionsBefore = usageSessionDao.getSessionsInRangeForApp(
            startDate = beforeStartDate,
            endDate = beforeEndDate,
            targetApp = outcome.targetApp
        )

        val totalUsagePrevious7Days = sessionsBefore
            .filter { it.endTimestamp != null }
            .sumOf { (it.endTimestamp!! - it.startTimestamp) / (60 * 1000L) }

        val avgDailyUsagePrevious7Days = if (sessionsBefore.isNotEmpty()) {
            totalUsagePrevious7Days / 7
        } else avgDailyUsageNext7Days  // Default to same

        // Determine usage change
        val weeklyUsageChange = when {
            avgDailyUsageNext7Days < avgDailyUsagePrevious7Days * 0.8 -> UsageChange.DECREASED
            avgDailyUsageNext7Days > avgDailyUsagePrevious7Days * 1.2 -> UsageChange.INCREASED
            else -> UsageChange.STABLE
        }

        // TODO: Check streak maintenance
        // Requires querying streak data
        val streakMaintained: Boolean? = null  // Placeholder

        // TODO: Check if app was uninstalled
        // Requires checking if app still exists in tracked apps
        val appUninstalled = false  // Placeholder

        // TODO: Check user retention (still using ThinkFast)
        // Requires checking recent app usage
        val userRetention = true  // Assume true for now

        return outcome.copy(
            avgDailyUsageNext7Days = avgDailyUsageNext7Days,
            weeklyUsageChange = weeklyUsageChange.name,
            streakMaintained = streakMaintained,
            appUninstalled = appUninstalled,
            userRetention = userRetention,
            longTermCollected = true,
            lastUpdated = System.currentTimeMillis()
        )
    }

    // ========== UTILITY FUNCTIONS ==========

    private fun getDayStart(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getDayEnd(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}

/**
 * Extension to convert entity to domain model
 */
private fun ComprehensiveOutcomeEntity.toDomainModel(): ComprehensiveInterventionOutcome {
    return ComprehensiveInterventionOutcome(
        interventionId = interventionId,
        sessionId = sessionId,
        targetApp = targetApp,
        timestamp = timestamp,
        immediateChoice = InterventionUserChoice.valueOf(immediateChoice),
        responseTime = responseTime,
        interactionDepth = InteractionDepth.valueOf(interactionDepth),
        sessionContinued = sessionContinued,
        sessionDurationAfter = sessionDurationAfter,
        quickReopen = quickReopen,
        switchedToProductiveApp = switchedToProductiveApp,
        reopenCount30Min = reopenCount30Min,
        totalUsageReductionToday = totalUsageReductionToday,
        goalMetToday = goalMetToday,
        additionalSessionsToday = additionalSessionsToday,
        totalScreenTimeToday = totalScreenTimeToday,
        weeklyUsageChange = weeklyUsageChange?.let { UsageChange.valueOf(it) },
        streakMaintained = streakMaintained,
        appUninstalled = appUninstalled,
        userRetention = userRetention,
        avgDailyUsageNext7Days = avgDailyUsageNext7Days,
        lastUpdated = lastUpdated,
        collectionStatus = OutcomeCollectionStatus(
            proximalCollected = proximalCollected,
            shortTermCollected = shortTermCollected,
            mediumTermCollected = mediumTermCollected,
            longTermCollected = longTermCollected
        )
    )
}
