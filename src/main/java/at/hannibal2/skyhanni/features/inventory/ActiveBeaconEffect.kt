package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ActiveBeaconEffect {

    val config get() = SkyHanniMod.feature.inventory

    private val patternGroup = RepoPattern.group("inventory.activebeaconeffect")
    private val inventoryPattern by patternGroup.pattern(
        "inventory",
        "Profile Stat Upgrades",
    )
    private val slotPattern by patternGroup.pattern(
        "slot.active",
        "§aActive stat boost!",
    )

    private var slot = -1
    private var inInventory = false

    @SubscribeEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        inInventory = inventoryPattern.matches(event.inventoryName)
        if (!inInventory) return

        for ((slot, stack) in event.inventoryItems) {
            if (stack.getLore().any { slotPattern.matches(it) }) {
                this.slot = slot
                return
            }
        }
        slot = -1
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        if (slot == -1) return

        val guiChest = event.gui
        val chest = guiChest.inventorySlots
        chest.getSlot(slot) highlight LorenzColor.GREEN
    }

    fun isEnabled() = config.highlightActiveBeaconEffect && IslandType.PRIVATE_ISLAND.isInIsland()
}
