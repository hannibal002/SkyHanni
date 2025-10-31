package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.ActionBarUpdateEvent
import at.hannibal2.hanni.events.ReceiveParticleEvent
import at.hannibal2.hanni.events.entity.EndermanTeleportEvent
import at.hannibal2.hanni.events.render.BlockOverlayRenderEvent
import at.hannibal2.hanni.events.render.OverlayType
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.util.EnumParticleTypes

/**
 *  I need these features in my dev env
 */
@HanniModule
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
        if (!HanniMod.feature.combat.mobs.endermanTeleportationHider) return
        event.cancel()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onActionBarUpdate(event: ActionBarUpdateEvent) {
        inChickenRace = IslandType.WINTER.isCurrent() && chickenRacePattern.matches(event.actionBar)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!HanniMod.feature.misc.hideExplosions) return
        if (inChickenRace) return

        when (event.type) {
            EnumParticleTypes.EXPLOSION_LARGE,
            EnumParticleTypes.EXPLOSION_HUGE,
            EnumParticleTypes.EXPLOSION_NORMAL,
            -> event.cancel()

            else -> return
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderBlockOverlay(event: BlockOverlayRenderEvent) {
        if (!HanniMod.feature.misc.hideFireOverlay) return

        if (event.overlayType == OverlayType.FIRE) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "mobs", "combat.mobs")
    }
}
