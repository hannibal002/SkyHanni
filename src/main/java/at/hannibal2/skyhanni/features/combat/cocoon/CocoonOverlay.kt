package at.hannibal2.skyhanni.features.combat.cocoon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.combat.cocoon.CocoonAPI.existingCocoons
import at.hannibal2.skyhanni.features.combat.cocoon.CocoonAPI.expectedLifetime
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.ExtendedChatColor
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText

@SkyHanniModule
object CocoonOverlay {
    private val config get() = SkyHanniMod.feature.combat.cocoonOverlay


    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        for (cocoon in existingCocoons) {
            if (config.showCocoonContainedMobName) event.drawDynamicText(
                cocoon.coordinates,
                cocoon.mob.name,
                2.0,
                yOff = 0f,
                seeThroughBlocks = cocoon.hasBeenSeen
            )
            if (config.showCocoonTimerTillHatch) {
                val timeLeft = expectedLifetime - cocoon.spawnTime.passedSince()
                val timeTillSpawn = timeLeft.format(showMilliSeconds = true)
                val percent =
                    (timeLeft.inPartialSeconds / expectedLifetime.inPartialSeconds).coerceAtLeast(0.0).coerceAtMost(1.0)
                val colour = ColorUtils.blendRGB(
                    LorenzColor.GREEN,
                    LorenzColor.RED,
                    percent
                )
                event.drawDynamicText(
                    cocoon.coordinates,
                    ExtendedChatColor(colour.rgb).asText(timeTillSpawn),
                    2.0,
                    yOff = -10f,
                    seeThroughBlocks = cocoon.hasBeenSeen
                )
            }
        }
    }

}
