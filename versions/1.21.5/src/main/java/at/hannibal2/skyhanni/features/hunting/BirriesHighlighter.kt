package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer

@SkyHanniModule
object BirriesHighlighter {

    val config get() = SkyHanniMod.feature.hunting.mobHighlight.birries

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
