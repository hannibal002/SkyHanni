package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.inventory.EquipmentApi
import at.hannibal2.skyhanni.features.inventory.EquipmentSlot
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.realDisplayName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ZorroCapeContestProtection {

    private val config get() = SkyHanniMod.feature.garden.jacobContest
    private val zorroCape = "ZORROS_CAPE".toInternalName()

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config.zorroCapeProtection) return
        if (!FarmingContestApi.inInventory) return

        val cloak = EquipmentApi.getEquipment(EquipmentSlot.CLOAK)?.getInternalNameOrNull()
        if (cloak == zorroCape) return

        val stack = event.item ?: return
        val claimableContest = JacobFarmingContestsInventory.isClaimableContest(stack)
        val isBulkClaim = stack.realDisplayName == "Bulk Claim"
        if (claimableContest || isBulkClaim) {
            event.cancel()
            notifyUser()
        }
    }

    private fun notifyUser() {
        TitleManager.sendTitle(
            titleText = "§cNo Zorro's Cape equipped!",
            duration = 2.seconds,
            location = TitleManager.TitleLocation.INVENTORY,
        )
        ChatUtils.notifyOrDisable(
            "§cBlocked Jacob's Contest reward claim! §eEquip §6Zorro's Cape §ewhen claiming contest rewards.",
            config::zorroCapeProtection,
        )
    }
}
