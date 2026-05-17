package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.InfernoMinionFuelsJson
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.features.minion.MinionFeatures.MINION_FUEL_SLOT
import at.hannibal2.skyhanni.features.minion.MinionFeatures.MINION_PICKUP_SLOT
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object InfernoMinionFeatures {
    private val config get() = SkyHanniMod.feature.misc.minions

    /**
     * REGEX-TEST: Inferno Minion II
     * REGEX-TEST: Inferno Minion IX
     */
    val infernoMinionTitlePattern by RepoPattern.pattern(
        "minion.infernominiontitle",
        "Inferno Minion .*",
    )
    var fuelItemIds = setOf<NeuInternalName>()
        private set
    private var inInventory = false

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<InfernoMinionFuelsJson>("InfernoMinionFuels")
        fuelItemIds = data.minionFuels.toSet()
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        inInventory = infernoMinionTitlePattern.matches(event.inventoryName)
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config.infernoFuelBlocker) return
        if (!inInventory) return

        val fuelItemName = event.container.getSlot(MINION_FUEL_SLOT).item.hoverName
        val containsFuel =
            NeuInternalName.fromItemNameOrNull(fuelItemName.formattedTextCompatLeadingWhiteLessResets()) in fuelItemIds
        if (!containsFuel) return

        if (event.slot?.index == MINION_FUEL_SLOT || event.slot?.index == MINION_PICKUP_SLOT) {
            if (KeyboardManager.isModifierKeyDown()) return
            event.cancel()
        }
    }

    @HandleEvent
    fun onToolTip(event: ToolTipTextEvent) {
        if (!config.infernoFuelBlocker) return
        if (!inInventory) return
        event.slot?.index ?: return

        val containsFuel = NeuInternalName.fromItemNameOrNull(event.itemStack.hoverName.formattedTextCompatLeadingWhiteLessResets()) in fuelItemIds
        if (!containsFuel) return

        if (event.slot.index == MINION_FUEL_SLOT) {
            event.toolTip.add("")
            event.toolTip.add("§c[SkyHanni] is blocking you from taking this out!")
            event.toolTip.add("  §7(Bypass by holding the ${KeyboardManager.getModifierKeyName()} key)")
        }
        if (event.slot.index == MINION_PICKUP_SLOT) {
            event.toolTip.add("")
            event.toolTip.add("§c[SkyHanni] is blocking you from picking this minion up!")
            event.toolTip.add("  §7(Bypass by holding the ${KeyboardManager.getModifierKeyName()} key)")
        }
    }
}
