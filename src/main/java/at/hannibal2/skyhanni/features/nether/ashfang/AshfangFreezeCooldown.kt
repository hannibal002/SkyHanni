package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AshfangFreezeCooldown {

    var lastHit = 0L

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return

        val message = event.message
        if (message.matchRegex("§cAshfang Follower's Cryogenic Blast hit you for (.*) damage!")) {
            lastHit = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        val duration = System.currentTimeMillis() - lastHit
        val maxDuration = 3_000

        val remainingLong = maxDuration - duration
        if (remainingLong > 0) {
            val format = TimeUtils.formatDuration(remainingLong, showMilliSeconds = true)
            SkyHanniMod.feature.ashfang.freezeCooldownPos.renderString(
                "§cAshfang Freeze: §a$format",
                posLabel = "Ashfang Freeze Cooldown"
            )
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && SkyHanniMod.feature.ashfang.freezeCooldown &&
                DamageIndicatorManager.isBossSpawned(BossType.NETHER_ASHFANG)
    }
}