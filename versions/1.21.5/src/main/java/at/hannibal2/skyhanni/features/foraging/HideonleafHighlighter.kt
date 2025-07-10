package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor

@SkyHanniModule
object HideonleafHighlighter {

    private val config get() = SkyHanniMod.feature.foraging.mobHighlight.hideonleaf

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onMob(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        if (mob.name != "Hideonleaf") return
        RenderLivingEntityHelper.setEntityColor(mob.baseEntity, config.color.toColor()) {
            isEnabled() && mob.distanceToPlayer() < 20
        }
    }

    private fun isEnabled() = config.enabled

    @HandleEvent
    fun onConfigFixEvent(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(91, "foraging.hideonleafHighlight", "foraging.mobHighlight.hideonleaf")
    }
}
