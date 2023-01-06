package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.matchRegex
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.text.DecimalFormat

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
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!isEnabled()) return
        val duration = System.currentTimeMillis() - lastHit
        val maxDuration = 3_000

        val remainingLong = maxDuration - duration
        if (remainingLong > 0) {
            val remaining = (remainingLong.toFloat() / 1000)
            val format = DecimalFormat("0.0").format(remaining + 0.1)
            SkyHanniMod.feature.ashfang.freezeCooldownPos.renderString("§cAshfang Freeze: §a${format}s")
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.ashfang.freezeCooldown &&
                DamageIndicatorManager.isBossSpawned(BossType.NETHER_ASHFANG)
    }
}