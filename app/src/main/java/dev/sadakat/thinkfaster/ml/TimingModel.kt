package dev.sadakat.thinkfaster.ml

import android.content.Context
import dev.sadakat.thinkfaster.util.ErrorLogger
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Calendar

/**
 * ML model for predicting optimal intervention timing
 * Uses TensorFlow Lite to predict intervention effectiveness based on:
 * - Time of day
 * - Day of week
 * - Recent usage patterns
 * - Past intervention outcomes
 */
class TimingModel(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var isModelLoaded = false

    // Feature indices
    private companion object {
        const val FEATURE_COUNT = 10
        const val FEATURE_HOUR_OF_DAY = 0
        const val FEATURE_DAY_OF_WEEK = 1
        const val FEATURE_IS_WEEKEND = 2
        const val FEATURE_RECENT_AVG_DURATION = 3
        const val FEATURE_RECENT_SESSION_COUNT = 4
        const val FEATURE_TIME_SINCE_LAST_INTERVENTION = 5
        const val FEATURE_LAST_INTERVENTION_EFFECTIVENESS = 6
        const val FEATURE_IS_WORK_HOURS = 7
        const val FEATURE_IS_EVENING = 8
        const val FEATURE_SESSION_FREQUENCY = 9
    }

    /**
     * Features for prediction
     */
    data class PredictionFeatures(
        val hourOfDay: Int,
        val dayOfWeek: Int,
        val recentAverageDuration: Float, // in minutes
        val recentSessionCount: Int,
        val timeSinceLastIntervention: Float, // in hours
        val lastInterventionEffectiveness: Float, // 0.0 to 1.0
        val sessionFrequency: Float // sessions per hour
    )

    /**
     * Prediction result
     */
    data class PredictionResult(
        val shouldShowIntervention: Boolean,
        val effectivenessScore: Float, // 0.0 to 1.0
        val confidence: Float // 0.0 to 1.0
    )

    /**
     * Initialize the model
     */
    fun initialize() {
        try {
            // Try to load the model from assets
            // For now, we'll use a fallback heuristic-based approach if model file doesn't exist
            loadModel()
        } catch (e: Exception) {
            ErrorLogger.warning(
                "TensorFlow Lite model not found, using heuristic-based predictions",
                context = "TimingModel.initialize"
            )
            isModelLoaded = false
        }
    }

    /**
     * Load TensorFlow Lite model from assets
     */
    private fun loadModel() {
        try {
            val assetFileDescriptor = context.assets.openFd("intervention_timing_model.tflite")
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            interpreter = Interpreter(modelBuffer)
            isModelLoaded = true

            ErrorLogger.info(
                "TensorFlow Lite model loaded successfully",
                context = "TimingModel.loadModel"
            )
        } catch (e: Exception) {
            ErrorLogger.warning(
                "Failed to load TensorFlow Lite model: ${e.message}",
                context = "TimingModel.loadModel"
            )
            isModelLoaded = false
        }
    }

    /**
     * Predict intervention effectiveness
     */
    fun predict(features: PredictionFeatures): PredictionResult {
        return if (isModelLoaded && interpreter != null) {
            predictWithModel(features)
        } else {
            predictWithHeuristics(features)
        }
    }

    /**
     * Predict using TensorFlow Lite model
     */
    private fun predictWithModel(features: PredictionFeatures): PredictionResult {
        try {
            // Prepare input buffer
            val inputBuffer = ByteBuffer.allocateDirect(FEATURE_COUNT * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            // Populate features
            val featureArray = FloatArray(FEATURE_COUNT)
            featureArray[FEATURE_HOUR_OF_DAY] = features.hourOfDay / 24f
            featureArray[FEATURE_DAY_OF_WEEK] = features.dayOfWeek / 7f
            featureArray[FEATURE_IS_WEEKEND] = if (features.dayOfWeek >= 6) 1f else 0f
            featureArray[FEATURE_RECENT_AVG_DURATION] = features.recentAverageDuration / 60f // normalize to hour
            featureArray[FEATURE_RECENT_SESSION_COUNT] = features.recentSessionCount / 10f // normalize
            featureArray[FEATURE_TIME_SINCE_LAST_INTERVENTION] = features.timeSinceLastIntervention / 24f // normalize to day
            featureArray[FEATURE_LAST_INTERVENTION_EFFECTIVENESS] = features.lastInterventionEffectiveness
            featureArray[FEATURE_IS_WORK_HOURS] = if (features.hourOfDay in 9..17) 1f else 0f
            featureArray[FEATURE_IS_EVENING] = if (features.hourOfDay in 18..22) 1f else 0f
            featureArray[FEATURE_SESSION_FREQUENCY] = features.sessionFrequency / 5f // normalize

            // Write to buffer
            featureArray.forEach { inputBuffer.putFloat(it) }

            // Prepare output buffer
            val outputBuffer = ByteBuffer.allocateDirect(2 * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)

            // Read output
            outputBuffer.rewind()
            val effectivenessScore = outputBuffer.float
            val confidence = outputBuffer.float

            val shouldShow = effectivenessScore >= 0.5f

            return PredictionResult(
                shouldShowIntervention = shouldShow,
                effectivenessScore = effectivenessScore,
                confidence = confidence
            )
        } catch (e: Exception) {
            ErrorLogger.error(
                e,
                message = "Error during model prediction",
                context = "TimingModel.predictWithModel"
            )
            // Fall back to heuristics
            return predictWithHeuristics(features)
        }
    }

    /**
     * Predict using heuristic rules (fallback when model is not available)
     */
    private fun predictWithHeuristics(features: PredictionFeatures): PredictionResult {
        var effectivenessScore = 0.5f
        var confidence = 0.7f

        // Avoid interventions during sleep hours
        if (features.hourOfDay < 7 || features.hourOfDay >= 23) {
            effectivenessScore = 0.1f
            confidence = 0.9f
        }
        // Prefer interventions during work hours for better attention
        else if (features.hourOfDay in 9..17) {
            effectivenessScore += 0.2f
        }
        // Evening interventions can be effective
        else if (features.hourOfDay in 18..22) {
            effectivenessScore += 0.15f
        }

        // Consider time since last intervention
        if (features.timeSinceLastIntervention < 0.5f) { // Less than 30 minutes
            effectivenessScore -= 0.3f // Too soon
        } else if (features.timeSinceLastIntervention > 4f) { // More than 4 hours
            effectivenessScore += 0.1f // Good gap
        }

        // Factor in past effectiveness
        effectivenessScore = (effectivenessScore + features.lastInterventionEffectiveness) / 2f

        // Consider session frequency
        if (features.sessionFrequency > 3f) { // Very frequent usage
            effectivenessScore += 0.1f // More important to intervene
        }

        // Clamp to valid range
        effectivenessScore = effectivenessScore.coerceIn(0f, 1f)

        val shouldShow = effectivenessScore >= 0.5f

        return PredictionResult(
            shouldShowIntervention = shouldShow,
            effectivenessScore = effectivenessScore,
            confidence = confidence
        )
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            interpreter?.close()
            interpreter = null
            isModelLoaded = false
        } catch (e: Exception) {
            ErrorLogger.error(
                e,
                message = "Error cleaning up model",
                context = "TimingModel.cleanup"
            )
        }
    }

    /**
     * Extract current features from calendar
     */
    fun extractCurrentTimeFeatures(
        recentAverageDuration: Float,
        recentSessionCount: Int,
        timeSinceLastIntervention: Float,
        lastInterventionEffectiveness: Float,
        sessionFrequency: Float
    ): PredictionFeatures {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday

        return PredictionFeatures(
            hourOfDay = hourOfDay,
            dayOfWeek = dayOfWeek,
            recentAverageDuration = recentAverageDuration,
            recentSessionCount = recentSessionCount,
            timeSinceLastIntervention = timeSinceLastIntervention,
            lastInterventionEffectiveness = lastInterventionEffectiveness,
            sessionFrequency = sessionFrequency
        )
    }
}
