package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.event.chocolatefactory.ChocolateFactoryAPI.getChocolateUpgradeCost
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ChocolateFactoryTooltip {

    private val config get() = ChocolateFactoryAPI.config
    private val profileStorage get() = ChocolateFactoryAPI.profileStorage

    // todo setting for this
    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        val slotIndex = event.slot.slotNumber
        if (slotIndex == ChocolateFactoryAPI.prestigeIndex) return
        if (slotIndex !in ChocolateFactoryAPI.otherUpgradeSlots && slotIndex !in ChocolateFactoryAPI.rabbitSlots) return

        val upgradeCost = event.toolTip.getChocolateUpgradeCost() ?: return
        val canAfford = upgradeCost <= ChocolateAmount.CURRENT.chocolate()

        event.toolTip.add("§8§m-----------------")
        val timeToUpgrade = ChocolateAmount.CURRENT.formattedTimeUntilGoal(upgradeCost)

        // get upgrade cost then add the time to reach that to the tooltip
        event.toolTip.add("§7Time until upgrade: §e$timeToUpgrade")

        if (slotIndex in listOf(
                ChocolateFactoryAPI.prestigeIndex,
                ChocolateFactoryAPI.handCookieIndex,
                ChocolateFactoryAPI.shrineIndex,
                ChocolateFactoryAPI.barnIndex
            )
        ) return

        // add cost per chocolate to tooltip

        // add how much more chocolate you get per day on average to tooltip

    }

}
