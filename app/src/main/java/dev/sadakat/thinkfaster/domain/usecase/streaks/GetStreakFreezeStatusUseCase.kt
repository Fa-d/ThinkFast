package dev.sadakat.thinkfaster.domain.usecase.streaks

import dev.sadakat.thinkfaster.data.preferences.StreakFreezePreferences
import dev.sadakat.thinkfaster.domain.model.StreakFreezeStatus

/**
 * Use case for retrieving the current streak freeze status
 * Returns information needed to display freeze UI and enable/disable freeze button
 */
class GetStreakFreezeStatusUseCase(
    private val freezePreferences: StreakFreezePreferences
) {
    operator fun invoke(targetApp: String): StreakFreezeStatus {
        val available = freezePreferences.getFreezesAvailable()
        val max = freezePreferences.getMaxMonthlyFreezes()
        val hasActive = freezePreferences.hasActiveFreezeForApp(targetApp)
        val activationDate = freezePreferences.getFreezeActivationDate(targetApp)

        return StreakFreezeStatus(
            freezesAvailable = available,
            maxFreezes = max,
            hasActiveFreeze = hasActive,
            freezeActivationDate = activationDate,
            canUseFreeze = available > 0 && !hasActive
        )
    }
}
