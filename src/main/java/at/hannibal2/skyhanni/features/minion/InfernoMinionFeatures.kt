package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.jsonobjects.repo.InfernoMinionFuelsJson
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class InfernoMinionFeatures {
    private val config get() = SkyHanniMod.feature.minions
    private val infernoMinionTitlePattern by RepoPattern.pattern(
        "minion.infernominiontitle",
        "Ice Minion .*"
    )
    private var fuelItemIds = listOf<NEUInternalName>()
    private var isInventory = false

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<InfernoMinionFuelsJson>("InfernoMinionFuels")
         fuelItemIds = data.inferno_minion_fuel ?: error("§cinferno_minion_fuel is missing from repo.")
    }

    @SubscribeEvent
    fun onMinionOpen(event: InventoryFullyOpenedEvent) {
        isInventory = infernoMinionTitlePattern.matches(event.inventoryName)
    }

    @SubscribeEvent
    fun onMinionClose(event: InventoryCloseEvent) {
        isInventory = false
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.infernoFuelBlocker) return
        if (!isInventory) return

        val containsFuel = NEUInternalName.fromItemNameOrNull(event.container.getSlot(19).stack.name.toString()) in fuelItemIds
        if (!containsFuel) return

        if (event.slot?.slotNumber == 19 || event.slot?.slotNumber == 53) {
            if (KeyboardManager.isModifierKeyDown()) return
            event.cancel()
        }
    }

    @SubscribeEvent
    fun blockedMessage(event: LorenzToolTipEvent) {
        if (!config.infernoFuelBlocker) return
        if (!isInventory) return

        val containsFuel = NEUInternalName.fromItemNameOrNull(event.itemStack.name.toString()) in fuelItemIds
        if (!containsFuel) return

        if (event.slot.slotNumber == 19) {
            event.toolTip.add("")
            event.toolTip.add("§c§l[SkyHanni] is blocking you from taking this out! (Hold CTRL to override)")
        }
        if (event.slot.slotNumber == 53) {
            event.toolTip.add("")
            event.toolTip.add("§c§l[SkyHanni] is blocking you from picking this minion up! (Hold CTRL to override)")
        }
    }
}
