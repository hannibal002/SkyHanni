package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

data class ChocolateFactoryUpgrade(
    val slotIndex: Int,
    val level: Int,
    val price: Long?,
    val extraPerSecond: Double? = null,
    val effectiveCost: Double? = null,
    val isRabbit: Boolean = false,
    val isPrestige: Boolean = false,
) {
    private var chocolateAmountType = ChocolateAmount.CURRENT
    val isMaxed = price == null
    var canAffordAt: SimpleTimeMark? = null

    init {
        if (isPrestige) {
            chocolateAmountType = ChocolateAmount.PRESTIGE
        }
        val canAffordIn = chocolateAmountType.timeUntilGoal(price ?: 0)
        canAffordAt = when {
            canAffordIn.isInfinite() -> SimpleTimeMark.farFuture()
            else -> SimpleTimeMark.now() + canAffordIn
        }
    }

    fun getNext(): ChocolateFactoryUpgrade? {
        if (price == null || isMaxed) return null

        // For now only get next level if it is a rabbit. Maybe add other stuff later.
        if (!isRabbit) return null

        val base = ChocolateFactoryAPI.rabbitUpgradeCostConstants[slotIndex]?.get("base") ?: return null
        val multiplier = ChocolateFactoryAPI.rabbitUpgradeCostConstants[slotIndex]?.get("multiplier") ?: return null
        val prestigeMultiplier = 1 + ChocolateFactoryAPI.prestigeMultiplier * ChocolateFactoryAPI.currentPrestige

        val nextPrice = floor(round(base * multiplier.pow((level + 1)) * prestigeMultiplier)).toLong()

        val next = ChocolateFactoryUpgrade(
            slotIndex, level + 1, nextPrice, extraPerSecond, nextPrice / (extraPerSecond ?: 1.0), isRabbit, isPrestige
        )

        return next
    }

    fun canAfford(): Boolean {
        if (price == null) return false
        return chocolateAmountType.chocolate() >= price
    }

    fun formattedTimeUntilGoal(): String {
        return chocolateAmountType.formattedTimeUntilGoal(price ?: 0)
    }

    fun stackTip(): String {
        return when {
            level == 0 -> ""
            isMaxed -> "§a✔"

            isRabbit -> when (level) {
                in (0..9) -> "$level"
                in (10..74) -> "§a$level"
                in (75..124) -> "§9$level"
                in (125..174) -> "§5$level"
                in (175..199) -> "§6$level"
                200 -> "§d$level"
                else -> "§c$level"
            }

            else -> "$level"
        }
    }

    fun getValidUpgradeIndex(): Int {
        return when (slotIndex) {
            in ignoredSlotIndexes -> -1
            else -> slotIndex
        }
    }

    companion object {
        var ignoredSlotIndexes = listOf<Int>()

        fun updateIgnoredSlots() {
            ignoredSlotIndexes = listOf(
                ChocolateFactoryAPI.prestigeIndex,
                ChocolateFactoryAPI.handCookieIndex,
                ChocolateFactoryAPI.shrineIndex,
                ChocolateFactoryAPI.barnIndex,
            )
        }
    }
}
