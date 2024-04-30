package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.event.chocolatefactory.ChocolateFactoryAPI.getChocolateUpgradeCost
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ChocolateFactoryTooltip {

    private val config get() = ChocolateFactoryAPI.config

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        if (!config.extraTooltipStats) return

        val slotIndex = event.slot.slotNumber
        if (slotIndex == ChocolateFactoryAPI.prestigeIndex) return
        if (slotIndex !in ChocolateFactoryAPI.otherUpgradeSlots && slotIndex !in ChocolateFactoryAPI.rabbitSlots) return

        val upgradeCost = event.toolTip.getChocolateUpgradeCost() ?: return

        event.toolTip.add("§8§m-----------------")
        val timeToUpgrade = ChocolateAmount.CURRENT.formattedTimeUntilGoal(upgradeCost)

        event.toolTip.add("§7Time until upgrade: §e$timeToUpgrade")

        if (slotIndex in listOf(
                ChocolateFactoryAPI.prestigeIndex,
                ChocolateFactoryAPI.handCookieIndex,
                ChocolateFactoryAPI.shrineIndex,
                ChocolateFactoryAPI.barnIndex
            )
        ) return

        val averageChocolate = ChocolateAmount.averageChocPerSecond().round(2)

        val newAverageChocolate = when (slotIndex) {
            in ChocolateFactoryAPI.rabbitSlots -> {
                val chocolateIncrease = ChocolateFactoryAPI.rabbitSlots[slotIndex] ?: 0
                ChocolateAmount.averageChocPerSecond(rawPerSecondIncrease = chocolateIncrease)
            }

            ChocolateFactoryAPI.timeTowerIndex -> ChocolateAmount.averageChocPerSecond(timeTowerLevelIncrease = 1)
            ChocolateFactoryAPI.coachRabbitIndex -> ChocolateAmount.averageChocPerSecond(baseMultiplierIncrease = 0.01)
            else -> averageChocolate
        }.round(2)

        val extra = (newAverageChocolate - averageChocolate).round(2)
        val ratioForUpgrade = (upgradeCost / extra).round(2)

        event.toolTip.add("§7Extra: §6$extra §7choc/s")
        event.toolTip.add("§7Effective Cost: §6${ratioForUpgrade.addSeparators()}")

        if (slotIndex != ChocolateFactoryAPI.timeTowerIndex) return
        event.toolTip.add("§7One charge will give: §6${ChocolateAmount.chocPerTimeTower().addSeparators()}")
    }
}
