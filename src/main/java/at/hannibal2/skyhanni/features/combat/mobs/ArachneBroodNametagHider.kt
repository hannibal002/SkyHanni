package at.hannibal2.skyhanni.features.combat.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object ArachneBroodNametagHider {

    private val config get() = SkyHanniMod.feature.combat.mobs.arachneSettings.boss

    @HandleEvent(priority = HandleEvent.HIGH, onlyOnIsland = IslandType.SPIDER_DEN)
    private fun onRenderLiving(event: SkyHanniRenderEntityEvent.Specials.Pre<ArmorStand>) {
        if (!config.hideNameTagOfBroods) return

        val entity = event.entity
        if (!entity.hasCustomName()) return

        val name = entity.name.formattedTextCompatLessResets()
        if (name.contains("§cArachne's Brood§r")) {
            event.cancel()
        }
    }
}
