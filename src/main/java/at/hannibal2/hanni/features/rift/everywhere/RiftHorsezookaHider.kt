package at.hannibal2.hanni.features.rift.everywhere

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.CheckRenderEntityEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import net.minecraft.entity.passive.EntityHorse

@HanniModule
object RiftHorsezookaHider {

    private val HORSEZOOKA = "HORSEZOOKA".toInternalName()

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onCheckRender(event: CheckRenderEntityEvent<EntityHorse>) {
        if (!HanniMod.feature.rift.horsezookaHider) return

        if (InventoryUtils.itemInHandId == HORSEZOOKA) {
            event.cancel()
        }
    }
}
