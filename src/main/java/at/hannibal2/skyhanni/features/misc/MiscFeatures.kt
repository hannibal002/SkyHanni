package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.entity.EndermanTeleportEvent
//#if TODO
import at.hannibal2.skyhanni.events.render.BlockOverlayRenderEvent
//#endif
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.util.EnumParticleTypes
//#if TODO
import net.minecraftforge.client.event.RenderBlockOverlayEvent
//#endif

// todo 1.21 impl needed
/**
 *  I need these features in my dev env
 */
@SkyHanniModule
object MiscFeatures {

    /**
     * REGEX-TEST: §6§LCHICKEN RACING §e00:26.842     §b2/9   §a§lJUMP
     */
    private val chickenRacePattern by RepoPattern.pattern(
        "misc.chickenrace.active",
        "(?:§.)*CHICKEN RACING.*",
    )

    private var inChickenRace = false

    @HandleEvent(onlyOnSkyblock = true)
    fun onEndermanTeleport(event: EndermanTeleportEvent) {
        if (!SkyHanniMod.feature.combat.mobs.endermanTeleportationHider) return
        event.cancel()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onActionBarUpdate(event: ActionBarUpdateEvent) {
        inChickenRace = IslandType.WINTER.isCurrent() && chickenRacePattern.matches(event.actionBar)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!SkyHanniMod.feature.misc.hideExplosions) return
        if (inChickenRace) return

        when (event.type) {
            EnumParticleTypes.EXPLOSION_LARGE,
            EnumParticleTypes.EXPLOSION_HUGE,
            EnumParticleTypes.EXPLOSION_NORMAL,
            -> event.cancel()

            else -> return
        }
    }

    //#if TODO
    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderBlockOverlay(event: BlockOverlayRenderEvent) {
        if (!SkyHanniMod.feature.misc.hideFireOverlay) return

        if (event.overlayType == RenderBlockOverlayEvent.OverlayType.FIRE) {
            event.cancel()
        }
    }
    //#endif

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "mobs", "combat.mobs")
    }
}
