package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.BossType
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AshfangFreezeCooldown {
    private val config get() = SkyHanniMod.feature.crimsonIsle.ashfang

    private var lastHit = 0L

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return

        val message = event.message
        if (message.matchRegex("§cAshfang Follower's Cryogenic Blast hit you for (.*) damage!")) {
            lastHit = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val duration = System.currentTimeMillis() - lastHit
        val maxDuration = 3_000

        val remainingLong = maxDuration - duration
        if (remainingLong > 0) {
            val format = TimeUtils.formatDuration(remainingLong, showMilliSeconds = true)
            config.freezeCooldownPos.renderString(
                "§cAshfang Freeze: §a$format",
                posLabel = "Ashfang Freeze Cooldown"
            )
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "ashfang.freezeCooldown", "crimsonIsle.ashfang.freezeCooldown")
        event.move(2, "ashfang.freezeCooldownPos", "crimsonIsle.ashfang.freezeCooldownPos")
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && config.freezeCooldown &&
                DamageIndicatorManager.isBossSpawned(BossType.NETHER_ASHFANG)
    }
}