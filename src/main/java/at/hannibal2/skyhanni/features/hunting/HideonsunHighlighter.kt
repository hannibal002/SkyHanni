package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor

@SkyHanniModule
object HideonsunHighlighter {

    private val config get() = SkyHanniMod.feature.hunting.mobHighlight.hideonsun

    @HandleEvent(onlyOnIsland = IslandType.TORRHUS_CANYON)
    private fun onMob(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        if (mob.name != "Hideonsun") return
        RenderLivingEntityHelper.setEntityColor(mob.baseEntity, config.color.toColor()) {
            isEnabled() && mob.distanceToPlayer() < 20
        }
    }

    private fun isEnabled() = config.enabled
}
