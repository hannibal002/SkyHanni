package at.hannibal2.hanni.features.combat.mobs

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.HanniRenderEntityEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import net.minecraft.entity.item.EntityArmorStand

@HanniModule
object ArachneMinisNametagHider {

    private val config get() = HanniMod.feature.combat.mobs

    @HandleEvent(priority = HandleEvent.HIGH, onlyOnIsland = IslandType.SPIDER_DEN)
    fun onRenderLiving(event: HanniRenderEntityEvent.Specials.Pre<EntityArmorStand>) {
        if (!config.hideNameTagArachneMinis) return

        val entity = event.entity
        if (!entity.hasCustomName()) return

        val name = entity.name
        if (name.contains("§cArachne's Brood§r")) {
            event.cancel()
        }
    }
}
