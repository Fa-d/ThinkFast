package dev.sadakat.thinkfaster.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.sadakat.thinkfaster.domain.intervention.ComprehensiveOutcomeTracker
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Phase 1: Worker for collecting short-term intervention outcomes
 *
 * Runs periodically (every 30 minutes) to collect outcomes for interventions
 * that happened 5-30 minutes ago.
 *
 * This worker:
 * - Finds interventions needing short-term collection
 * - Queries session data to determine if user continued using app
 * - Records quick reopens and session duration
 * - Updates outcome records with short-term data
 */
class ShortTermOutcomeCollectionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val comprehensiveOutcomeTracker: ComprehensiveOutcomeTracker by inject()

    override suspend fun doWork(): Result {
        return try {
            dev.sadakat.thinkfaster.util.ErrorLogger.debug(
                message = "ShortTermOutcomeCollectionWorker started",
                context = "OutcomeCollection"
            )

            // Collect pending short-term outcomes (5-30 min after intervention)
            comprehensiveOutcomeTracker.collectPendingShortTermOutcomes(limit = 50)

            dev.sadakat.thinkfaster.util.ErrorLogger.debug(
                message = "ShortTermOutcomeCollectionWorker completed successfully",
                context = "OutcomeCollection"
            )

            Result.success()
        } catch (e: Exception) {
            dev.sadakat.thinkfaster.util.ErrorLogger.warning(
                message = "ShortTermOutcomeCollectionWorker failed: ${e.message}",
                context = "OutcomeCollection"
            )

            // Retry on failure (up to 3 attempts)
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "short_term_outcome_collection"
    }
}

/**
 * Phase 1: Worker for collecting medium-term intervention outcomes
 *
 * Runs periodically (every 6 hours) to collect outcomes for interventions
 * that happened earlier the same day.
 *
 * This worker:
 * - Finds interventions needing medium-term collection
 * - Calculates total usage for the day
 * - Records goal achievement
 * - Counts additional sessions after intervention
 */
class MediumTermOutcomeCollectionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val comprehensiveOutcomeTracker: ComprehensiveOutcomeTracker by inject()

    override suspend fun doWork(): Result {
        return try {
            dev.sadakat.thinkfaster.util.ErrorLogger.debug(
                message = "MediumTermOutcomeCollectionWorker started",
                context = "OutcomeCollection"
            )

            // Collect pending medium-term outcomes (same day, after 1 hour)
            comprehensiveOutcomeTracker.collectPendingMediumTermOutcomes(limit = 50)

            dev.sadakat.thinkfaster.util.ErrorLogger.debug(
                message = "MediumTermOutcomeCollectionWorker completed successfully",
                context = "OutcomeCollection"
            )

            Result.success()
        } catch (e: Exception) {
            dev.sadakat.thinkfaster.util.ErrorLogger.warning(
                message = "MediumTermOutcomeCollectionWorker failed: ${e.message}",
                context = "OutcomeCollection"
            )

            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "medium_term_outcome_collection"
    }
}

/**
 * Phase 1: Worker for collecting long-term intervention outcomes
 *
 * Runs daily to collect outcomes for interventions that happened 7+ days ago.
 *
 * This worker:
 * - Finds interventions needing long-term collection
 * - Calculates weekly usage changes
 * - Checks streak maintenance
 * - Determines user retention
 * - Calculates comprehensive reward scores
 */
class LongTermOutcomeCollectionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val comprehensiveOutcomeTracker: ComprehensiveOutcomeTracker by inject()

    override suspend fun doWork(): Result {
        return try {
            dev.sadakat.thinkfaster.util.ErrorLogger.debug(
                message = "LongTermOutcomeCollectionWorker started",
                context = "OutcomeCollection"
            )

            // Collect pending long-term outcomes (7-30 days after intervention)
            comprehensiveOutcomeTracker.collectPendingLongTermOutcomes(limit = 50)

            // After collecting long-term outcomes, update reward scores
            // This is important for RL algorithms in Phase 4
            // (Reward score calculation is done automatically in the tracker)

            dev.sadakat.thinkfaster.util.ErrorLogger.debug(
                message = "LongTermOutcomeCollectionWorker completed successfully",
                context = "OutcomeCollection"
            )

            Result.success()
        } catch (e: Exception) {
            dev.sadakat.thinkfaster.util.ErrorLogger.warning(
                message = "LongTermOutcomeCollectionWorker failed: ${e.message}",
                context = "OutcomeCollection"
            )

            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "long_term_outcome_collection"
    }
}
