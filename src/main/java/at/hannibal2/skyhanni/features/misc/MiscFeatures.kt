package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.entity.EndermanTeleportEvent
import at.hannibal2.skyhanni.events.render.BlockOverlayRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.client.event.RenderBlockOverlayEvent

/**
 *  I need these features in my dev env
 */
@SkyHanniModule
object MiscFeatures {

    @HandleEvent(onlyOnSkyblock = true)
    fun onEndermanTeleport(event: EndermanTeleportEvent) {
        if (!SkyHanniMod.feature.combat.mobs.endermanTeleportationHider) return
        event.cancel()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!SkyHanniMod.feature.misc.hideExplosions) return

        when (event.type) {
            EnumParticleTypes.EXPLOSION_LARGE,
            EnumParticleTypes.EXPLOSION_HUGE,
            EnumParticleTypes.EXPLOSION_NORMAL,
            -> event.cancel()

            else -> {}
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderBlockOverlay(event: BlockOverlayRenderEvent) {
        if (!SkyHanniMod.feature.misc.hideFireOverlay) return

        if (event.overlayType == RenderBlockOverlayEvent.OverlayType.FIRE) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "mobs", "combat.mobs")
    }
}
