package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getAllItems
import at.hannibal2.skyhanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ActiveBeaconEffect {

    val config get() = SkyHanniMod.feature.inventory

    private val patternGroup = RepoPattern.group("inventory.activebeaconeffect")
    private val inventoryPattern by patternGroup.pattern(
        "inventory",
        "Profile Stat Upgrades"
    )
    private val slotPattern by patternGroup.pattern(
        "slot.active",
        "§aActive stat boost!"
    )


    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!inventoryPattern.matches(InventoryUtils.openInventoryName())) return

        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest
        for ((slot, _) in chest.getUpperItems()) {
            if (slot.stack.getLore().any { slotPattern.matches(it) }) {
                slot highlight LorenzColor.GREEN
                break
            }
        }
    }

    fun isEnabled() = config.highlightActiveBeaconEffect && IslandType.PRIVATE_ISLAND.isInIsland()
}
