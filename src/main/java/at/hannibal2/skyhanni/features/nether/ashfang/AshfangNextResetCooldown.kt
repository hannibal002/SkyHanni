package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object AshfangNextResetCooldown {

    private val config get() = AshfangManager.config
    private val ashfangResetTime = 46.1.seconds

    @HandleEvent
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled() || AshfangManager.lastSpawnTime.isFarPast()) return
        val nextSpawn = AshfangManager.lastSpawnTime + ashfangResetTime

        val format = if (nextSpawn.isInPast()) "§aNow!"
        else "§b${nextSpawn.timeUntil().format(TimeUnit.SECOND, showMilliSeconds = true)}"

        val display = Renderable.text("§cAshfang next reset in: $format")
        config.nextResetCooldownPos.renderRenderable(display, posLabel = "Ashfang Reset Cooldown")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "ashfang.nextResetCooldown", "crimsonIsle.ashfang.nextResetCooldown")
        event.move(2, "ashfang.nextResetCooldownPos", "crimsonIsle.ashfang.nextResetCooldownPos")
    }

    private fun isEnabled() = AshfangManager.active && config.nextResetCooldown
}
