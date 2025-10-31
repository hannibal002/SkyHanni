package at.hannibal2.hanni.features.hunting

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.MobEvent
import at.hannibal2.hanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.toColor
import at.hannibal2.hanni.utils.LocationUtils.distanceToPlayer

@HanniModule
object BirriesHighlighter {

    val config get() = HanniMod.feature.hunting.mobHighlight.birries

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onMob(event: MobEvent.Spawn.SkyblockMob) {
        if (event.mob.name != "Birries") return
        RenderLivingEntityHelper.setEntityColor(
            event.mob.baseEntity, config.color.toColor()
        ) { isEnabled() && event.mob.baseEntity.distanceToPlayer() < 10 }
    }

    fun isEnabled() = config.enabled

    @HandleEvent
    fun onConfigFixEvent(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(91, "foraging.birriesHighlight", "foraging.mobHighlight.birries")
        event.move(100, "foraging.mobHighlight.birries", "hunting.mobHighlight.birries")
    }
}
