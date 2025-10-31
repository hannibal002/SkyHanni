package at.hannibal2.hanni.features.hunting

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.MobEvent
import at.hannibal2.hanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.toColor

@HanniModule
object HideonleafHighlighter {

    private val config get() = HanniMod.feature.hunting.mobHighlight.hideonleaf

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
        event.move(100, "foraging.mobHighlight.hideonleaf", "hunting.mobHighlight.hideonleaf")
    }
}
