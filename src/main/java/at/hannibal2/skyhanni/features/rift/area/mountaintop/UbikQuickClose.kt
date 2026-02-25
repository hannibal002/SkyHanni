package at.hannibal2.skyhanni.features.rift.area.mountaintop

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.item.Items

@SkyHanniModule
object UbikQuickClose {

    private val config get() = RiftApi.config.area.mountaintop

    private val patternGroup = RepoPattern.group("rift.ubik")

    private val inventoryNamePattern by patternGroup.pattern(
        "inventory-name",
        "Split or Steal",
    )

    private val inventory = InventoryDetector { name -> inventoryNamePattern.matches(name) }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config.ubikQuickClose) return
        if (!inventory.isInside()) return

        if (!event.container.slots[4].item.`is`(Items.CLOCK)) {
            event.gui.onClose()
        }
    }
}
