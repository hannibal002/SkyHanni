package at.hannibal2.skyhanni.features.rift.everywhere

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import net.minecraft.entity.passive.EntityHorse

@SkyHanniModule
object RiftHorsezookaHider {

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onCheckRender(event: CheckRenderEntityEvent<EntityHorse>) {
        if (!SkyHanniMod.feature.rift.horsezookaHider) return

        if (InventoryUtils.itemInHandId.equals("HORSEZOOKA")) {
            event.cancel()
        }
    }
}
