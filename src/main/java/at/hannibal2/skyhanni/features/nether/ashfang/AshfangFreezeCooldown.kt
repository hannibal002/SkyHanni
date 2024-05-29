package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.BossType
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object AshfangFreezeCooldown {

    private val config get() = SkyHanniMod.feature.crimsonIsle.ashfang

    private val cryogenicBlastPattern by RepoPattern.pattern(
        "ashfang.freeze.cryogenic",
        "§cAshfang Follower's Cryogenic Blast hit you for .* damage!"
    )

    private var lastHit = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        val message = event.message
        cryogenicBlastPattern.matchMatcher(message) {
            lastHit = SimpleTimeMark.now()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        val passedSince = lastHit.passedSince()
        val maxDuration = 3.seconds
        val duration = maxDuration - passedSince
        if (duration > 0.seconds) {
            val format = duration.format(showMilliSeconds = true)
            config.freezeCooldownPos.renderString(
                "§cAshfang Freeze: §a$format",
                posLabel = "Ashfang Freeze Cooldown"
            )
        }
    }

    fun isCurrentlyFrozen(): Boolean {
        val passedSince = lastHit.passedSince()
        val maxDuration = 3.seconds
        val duration = maxDuration - passedSince
        return duration > 0.seconds
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "ashfang.freezeCooldown", "crimsonIsle.ashfang.freezeCooldown")
        event.move(2, "ashfang.freezeCooldownPos", "crimsonIsle.ashfang.freezeCooldownPos")
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.freezeCooldown &&
        DamageIndicatorManager.isBossSpawned(BossType.NETHER_ASHFANG)
}
