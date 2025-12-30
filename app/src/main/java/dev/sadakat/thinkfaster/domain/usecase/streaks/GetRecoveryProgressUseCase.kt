package dev.sadakat.thinkfaster.domain.usecase.streaks

import dev.sadakat.thinkfaster.domain.model.StreakRecovery
import dev.sadakat.thinkfaster.domain.repository.StreakRecoveryRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for retrieving recovery progress for a specific app
 * Returns null if no active recovery
 */
class GetRecoveryProgressUseCase(
    private val streakRecoveryRepository: StreakRecoveryRepository
) {
    /**
     * Get current recovery status (one-time)
     */
    suspend operator fun invoke(targetApp: String): StreakRecovery? {
        return streakRecoveryRepository.getRecoveryByApp(targetApp)
    }

    /**
     * Observe recovery status (reactive)
     */
    fun observe(targetApp: String): Flow<StreakRecovery?> {
        return streakRecoveryRepository.observeRecoveryByApp(targetApp)
    }
}
