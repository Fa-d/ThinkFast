package dev.sadakat.thinkfaster.domain.usecase.insights

import dev.sadakat.thinkfaster.domain.model.BehavioralInsights
import dev.sadakat.thinkfaster.domain.model.ComparativeAnalytics
import dev.sadakat.thinkfaster.domain.model.InsightPriority
import dev.sadakat.thinkfaster.domain.model.InterventionInsights
import dev.sadakat.thinkfaster.domain.model.PredictiveInsights
import dev.sadakat.thinkfaster.domain.model.SmartInsight
import dev.sadakat.thinkfaster.domain.model.SmartInsightBuilder
import dev.sadakat.thinkfaster.domain.model.StatsPeriod

/**
 * Use case for generating the single most relevant smart insight
 * Phase 5: Intelligent insight selection using priority algorithm
 *
 * Priority Algorithm:
 * 1. URGENT: Streak at risk, goal almost reached (immediate action needed)
 * 2. HIGH: Behavioral patterns discovered (quick reopens, late night, weekend)
 * 3. MEDIUM: Intervention effectiveness, personal bests, improvements
 * 4. LOW: General awareness (on track, peak times)
 *
 * Within same priority:
 * - Actionable insights ranked higher
 * - Recent patterns preferred over historical
 * - Specific over general
 */
class GenerateSmartInsightUseCase(
    private val calculateBehavioralInsightsUseCase: CalculateBehavioralInsightsUseCase,
    private val calculateInterventionInsightsUseCase: CalculateInterventionInsightsUseCase,
    private val generatePredictiveInsightsUseCase: GeneratePredictiveInsightsUseCase,
    private val calculateComparativeAnalyticsUseCase: CalculateComparativeAnalyticsUseCase
) {
    /**
     * Generate the single most relevant smart insight for the user RIGHT NOW
     * @param targetApp Package name of the app (optional, uses all if null)
     */
    suspend operator fun invoke(targetApp: String? = null): SmartInsight {
        // Gather all available insights
        val behavioral = calculateBehavioralInsightsUseCase(StatsPeriod.WEEKLY)
        val intervention = calculateInterventionInsightsUseCase(StatsPeriod.WEEKLY)
        val predictive = generatePredictiveInsightsUseCase(targetApp)
        val comparative = calculateComparativeAnalyticsUseCase(targetApp)

        // Generate candidate insights from each source
        val candidates = mutableListOf<SmartInsight>()

        // PRIORITY 1: URGENT - Predictive insights (streak risk, goal achievement)
        predictive?.let { pred ->
            candidates.addAll(generatePredictiveInsights(pred))
        }

        // PRIORITY 2: HIGH - Behavioral patterns (actionable)
        behavioral?.let { behav ->
            candidates.addAll(generateBehavioralInsights(behav))
        }

        // PRIORITY 3: MEDIUM - Intervention effectiveness & Comparative analytics
        intervention?.let { interv ->
            candidates.addAll(generateInterventionInsights(interv))
        }
        comparative?.let { comp ->
            candidates.addAll(generateComparativeInsights(comp))
        }

        // PRIORITY 4: LOW - General awareness
        if (predictive != null && !predictive.streakAtRisk && predictive.willMeetGoal) {
            candidates.add(
                SmartInsightBuilder.onTrack(predictive.projectedEndOfDayUsageMinutes)
            )
        }

        // Apply priority algorithm to select the best insight
        return selectTopInsight(candidates)
    }

    /**
     * Generate insights from predictive data (URGENT priority)
     */
    private fun generatePredictiveInsights(predictive: PredictiveInsights): List<SmartInsight> {
        val insights = mutableListOf<SmartInsight>()

        // Streak at risk (URGENT)
        if (predictive.streakAtRisk && predictive.currentUsagePercentage >= 80) {
            insights.add(
                SmartInsightBuilder.streakAtRisk(predictive.currentUsagePercentage)
            )
        }

        // Goal almost reached (URGENT - positive reinforcement)
        if (predictive.currentUsagePercentage >= 90 && !predictive.streakAtRisk) {
            val remaining = ((100 - predictive.currentUsagePercentage) *
                predictive.projectedEndOfDayUsageMinutes / 100).toInt()
            insights.add(
                SmartInsightBuilder.goalAlmostReached(remaining)
            )
        }

        // High-risk time ahead (HIGH if next risk time is within 2 hours)
        predictive.nextHighRiskTime?.let { riskTime ->
            if (riskTime.confidence >= 0.6f) {
                // Create a custom high-risk time insight
                insights.add(
                    SmartInsight(
                        type = dev.sadakat.thinkfaster.domain.model.InsightType.HIGH_RISK_TIME_AHEAD,
                        priority = InsightPriority.HIGH,
                        message = "You usually open apps around ${riskTime.timeLabel}",
                        icon = "⏰",
                        details = "You've opened apps ${riskTime.historicalFrequency} times at this hour over the past 2 weeks.",
                        actionable = true,
                        contextData = mapOf(
                            "hour" to riskTime.hourOfDay,
                            "frequency" to riskTime.historicalFrequency,
                            "avgMinutes" to riskTime.averageUsageMinutes
                        )
                    )
                )
            }
        }

        return insights
    }

    /**
     * Generate insights from behavioral patterns (HIGH priority)
     */
    private fun generateBehavioralInsights(behavioral: BehavioralInsights): List<SmartInsight> {
        val insights = mutableListOf<SmartInsight>()

        // Quick reopens (HIGH - most actionable)
        if (behavioral.quickReopenCount > 3) {
            insights.add(
                SmartInsightBuilder.quickReopenPattern(behavioral.quickReopenCount)
            )
        }

        // Late night pattern (HIGH if significant)
        if (behavioral.lateNightSessionCount >= 3) {
            val lateNightMinutes = behavioral.lateNightUsageMinutes
            insights.add(
                SmartInsightBuilder.lateNightPattern(
                    behavioral.lateNightSessionCount,
                    lateNightMinutes
                )
            )
        }

        // Weekend pattern (HIGH if difference > 30%)
        if (behavioral.weekendVsWeekdayRatio > 1.3f) {
            val percentHigher = ((behavioral.weekendVsWeekdayRatio - 1.0f) * 100).toInt()
            insights.add(
                SmartInsightBuilder.weekendPattern(percentHigher)
            )
        }

        // Peak time (LOW - general awareness)
        insights.add(
            SmartInsightBuilder.peakTimePattern(behavioral.peakUsageContext)
        )

        return insights
    }

    /**
     * Generate insights from intervention effectiveness (MEDIUM priority)
     */
    private fun generateInterventionInsights(intervention: InterventionInsights): List<SmartInsight> {
        val insights = mutableListOf<SmartInsight>()

        // Most effective intervention type (MEDIUM)
        intervention.mostEffectiveType?.let { effective ->
            if (effective.successRate >= 60) {
                insights.add(
                    SmartInsightBuilder.interventionSuccess(
                        effective.displayName,
                        effective.successRate.toInt()
                    )
                )
            }
        }

        // Improving effectiveness (MEDIUM - positive reinforcement)
        if (intervention.trendingUp) {
            insights.add(
                SmartInsight(
                    type = dev.sadakat.thinkfaster.domain.model.InsightType.IMPROVING_EFFECTIVENESS,
                    priority = InsightPriority.MEDIUM,
                    message = "Interventions are becoming more effective over time",
                    icon = "📈",
                    details = "You're getting better at responding to reminders and going back.",
                    actionable = false,
                    contextData = mapOf("successRate" to intervention.overallSuccessRate)
                )
            )
        }

        return insights
    }

    /**
     * Generate insights from comparative analytics (MEDIUM priority)
     */
    private fun generateComparativeInsights(comparative: ComparativeAnalytics): List<SmartInsight> {
        val insights = mutableListOf<SmartInsight>()

        // Personal best streak (MEDIUM)
        if (comparative.personalBests.longestStreak >= 7) {
            insights.add(
                SmartInsightBuilder.personalBest(comparative.personalBests.longestStreak)
            )
        }

        // Improvement since goal set (MEDIUM - positive reinforcement)
        comparative.improvementRate?.let { improvement ->
            if (improvement > 10f) {
                insights.add(
                    SmartInsightBuilder.improvementSinceGoal(improvement.toInt())
                )
            }
        }

        // Best week comparison (MEDIUM)
        comparative.personalBests.lowestUsageWeek?.let { bestWeek ->
            if (bestWeek.percentBelowAverage != null && bestWeek.percentBelowAverage >= 20) {
                insights.add(
                    SmartInsight(
                        type = dev.sadakat.thinkfaster.domain.model.InsightType.THIS_WEEK_VS_BEST,
                        priority = InsightPriority.MEDIUM,
                        message = "Your best week was ${bestWeek.formatUsage()}",
                        icon = "🌟",
                        details = "That's ${bestWeek.percentBelowAverage}% below your average.",
                        actionable = false,
                        contextData = mapOf(
                            "minutes" to bestWeek.usageMinutes,
                            "percentBelow" to bestWeek.percentBelowAverage
                        )
                    )
                )
            }
        }

        return insights
    }

    /**
     * Priority algorithm to select the single best insight
     *
     * Selection criteria (in order):
     * 1. Priority level (URGENT > HIGH > MEDIUM > LOW)
     * 2. Actionability (actionable insights preferred)
     * 3. First match within priority group
     */
    private fun selectTopInsight(candidates: List<SmartInsight>): SmartInsight {
        if (candidates.isEmpty()) {
            // Fallback: no data yet
            return SmartInsight(
                type = dev.sadakat.thinkfaster.domain.model.InsightType.NO_DATA_YET,
                priority = InsightPriority.LOW,
                message = "Keep using the app to see insights",
                icon = "📊",
                details = "We'll show you patterns and recommendations as you track your usage.",
                actionable = false,
                contextData = emptyMap()
            )
        }

        // Sort by priority (URGENT first) and actionability
        val sorted = candidates.sortedWith(
            compareBy<SmartInsight> { insight ->
                when (insight.priority) {
                    InsightPriority.URGENT -> 0
                    InsightPriority.HIGH -> 1
                    InsightPriority.MEDIUM -> 2
                    InsightPriority.LOW -> 3
                }
            }.thenByDescending { it.actionable }
        )

        return sorted.first()
    }
}
