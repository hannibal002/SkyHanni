package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.features.event.chocolatefactory.ChocolateFactoryAPI.profileStorage
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class ChocolateAmount(val chocolate: () -> Long) {
    CURRENT({ profileStorage?.currentChocolate ?: 0 }),
    PRESTIGE({ profileStorage?.chocolateThisPrestige ?: 0 }),
    ALL_TIME({ profileStorage?.chocolateAllTime ?: 0 }),
    ;

    val formatted get(): String = chocolate().addSeparators()

    fun timeUntilGoal(goal: Long): Duration {
        val profileStorage = ChocolateFactoryAPI.profileStorage ?: return Duration.ZERO

        val updatedAgo = SimpleTimeMark(profileStorage.lastDataSave).passedSince().inWholeSeconds

        val baseMultiplier = profileStorage.rawChocolateMultiplier
        val baseChocolatePerSecond = profileStorage.rawChocPerSecond
        val timeTowerMultiplier = baseMultiplier + profileStorage.timeTowerLevel * 0.1

        var needed = goal - chocolate()
        val secondsUntilTowerExpires = ChocolateFactoryTimeTowerManager.timeTowerActiveDuration().inWholeSeconds

        val timeTowerChocPerSecond = baseChocolatePerSecond * timeTowerMultiplier

        val secondsAtRate = needed / timeTowerChocPerSecond
        if (secondsAtRate < secondsUntilTowerExpires) {
            return secondsAtRate.seconds - updatedAgo.seconds
        }

        needed -= (secondsUntilTowerExpires * timeTowerChocPerSecond).toLong()
        val chocPerSecond = baseChocolatePerSecond * baseMultiplier
        return (needed / chocPerSecond + secondsUntilTowerExpires).seconds - updatedAgo.seconds
    }
}
