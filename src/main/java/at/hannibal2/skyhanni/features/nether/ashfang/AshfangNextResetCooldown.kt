package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object AshfangNextResetCooldown {

    private val config get() = AshfangManager.config

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val nextSpawn = AshfangManager.nextSpawnTime
        if (nextSpawn.isFarPast()) return

        val format = if (nextSpawn.isInPast()) "§aNow!"
        else "§b${nextSpawn.timeUntil().format(TimeUnit.SECOND, showMilliSeconds = true)}"

        config.nextResetCooldownPos.renderString(
            "§cAshfang next reset in: $format",
            posLabel = "Ashfang Reset Cooldown",
        )
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "ashfang.nextResetCooldown", "crimsonIsle.ashfang.nextResetCooldown")
        event.move(2, "ashfang.nextResetCooldownPos", "crimsonIsle.ashfang.nextResetCooldownPos")
    }

    private fun isEnabled() = AshfangManager.isAshfangActive() && config.nextResetCooldown
}
