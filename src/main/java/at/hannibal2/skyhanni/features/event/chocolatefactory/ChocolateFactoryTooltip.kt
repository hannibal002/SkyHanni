package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.event.chocolatefactory.ChocolateFactoryAPI.profileStorage
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ChocolateFactoryTooltip {

    private val config get() = ChocolateFactoryAPI.config

    private val ignoredSlotIndexes = mutableListOf<Int>()

    fun updateIgnoredSlots() {
        ignoredSlotIndexes.clear()
        ignoredSlotIndexes.add(ChocolateFactoryAPI.prestigeIndex)
        ignoredSlotIndexes.add(ChocolateFactoryAPI.handCookieIndex)
        ignoredSlotIndexes.add(ChocolateFactoryAPI.shrineIndex)
        ignoredSlotIndexes.add(ChocolateFactoryAPI.barnIndex)
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        if (!config.extraTooltipStats) return

        val slotIndex = event.slot.slotNumber
        if (slotIndex == ChocolateFactoryAPI.prestigeIndex) return
        if (slotIndex !in ChocolateFactoryAPI.otherUpgradeSlots && slotIndex !in ChocolateFactoryAPI.rabbitSlots) return

        val upgradeCost = ChocolateFactoryAPI.getChocolateUpgradeCost(event.toolTip) ?: return

        event.toolTip.add("§8§m-----------------")
        val timeToUpgrade = ChocolateAmount.CURRENT.formattedTimeUntilGoal(upgradeCost)

        event.toolTip.add("§7Time until upgrade: §e$timeToUpgrade")

        if (slotIndex in ignoredSlotIndexes) return

        val averageChocolate = averageChocPerSecond().round(2)

        val newAverageChocolate = when (slotIndex) {
            in ChocolateFactoryAPI.rabbitSlots -> {
                val chocolateIncrease = ChocolateFactoryAPI.rabbitSlots[slotIndex] ?: 0
                averageChocPerSecond(rawPerSecondIncrease = chocolateIncrease)
            }

            ChocolateFactoryAPI.timeTowerIndex -> averageChocPerSecond(timeTowerLevelIncrease = 1)
            ChocolateFactoryAPI.coachRabbitIndex -> averageChocPerSecond(baseMultiplierIncrease = 0.01)
            else -> averageChocolate
        }.round(2)

        val extra = (newAverageChocolate - averageChocolate).round(2)
        val ratioForUpgrade = (upgradeCost / extra).round(2)

        event.toolTip.add("§7Extra: §6$extra §7choc/s")
        event.toolTip.add("§7Effective Cost: §6${ratioForUpgrade.addSeparators()}")

        if (slotIndex == ChocolateFactoryAPI.timeTowerIndex) {
            event.toolTip.add("§7One charge will give: §6${chocPerTimeTower().addSeparators()}")
        }
    }

    private fun averageChocPerSecond(
        baseMultiplierIncrease: Double = 0.0,
        rawPerSecondIncrease: Int = 0,
        timeTowerLevelIncrease: Int = 0,
    ): Double {
        val profileStorage = profileStorage ?: return 0.0

        val baseMultiplier = profileStorage.chocolateMultiplier + baseMultiplierIncrease
        val rawPerSecond = profileStorage.rawChocPerSecond + rawPerSecondIncrease
        val timeTowerLevel = profileStorage.timeTowerLevel + timeTowerLevelIncrease

        val timeTowerCooldown = profileStorage.timeTowerCooldown

        val basePerSecond = rawPerSecond * baseMultiplier
        val towerCalc = (rawPerSecond * timeTowerLevel * .1) / timeTowerCooldown

        return basePerSecond + towerCalc
    }

    private fun chocPerTimeTower(): Int {
        val profileStorage = profileStorage ?: return 0
        val amountPerSecond = profileStorage.rawChocPerSecond * profileStorage.timeTowerLevel * .1
        val amountPerHour = amountPerSecond * 60 * 60
        return amountPerHour.toInt()
    }
}
