package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object AshfangFreezeCooldown {

    private val config get() = AshfangManager.config

    /**
     * REGEX-TEST: Ashfang Follower's Cryogenic Blast hit you for 1,234 damage!
     */
    private val cryogenicBlastPattern by RepoPattern.pattern(
        "ashfang.freeze.cryogenic.colorless",
        "Ashfang Follower's Cryogenic Blast hit you for [\\d,.]+ damage!",
    )

    private var unfrozenTime = SimpleTimeMark.farPast()
    private val freezeDuration = 3.seconds

    @HandleEvent
    private fun onChat(event: SystemMessageEvent.Allow) {
        if (!isEnabled()) return
        if (cryogenicBlastPattern.matches(event.cleanMessage)) unfrozenTime = SimpleTimeMark.now() + freezeDuration
    }

    @HandleEvent
    private fun onGuiRenderOverlay() {
        if (!isEnabled() || !isCurrentlyFrozen()) return

        val format = unfrozenTime.timeUntil().format(showMilliSeconds = true)
        val display = Renderable.text("§cAshfang Freeze: §a$format")

        config.freezeCooldownPos.renderRenderable(display, posLabel = "Ashfang Freeze Cooldown")
    }

    fun isCurrentlyFrozen() = unfrozenTime.isInFuture()

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "ashfang.freezeCooldown", "crimsonIsle.ashfang.freezeCooldown")
        event.move(2, "ashfang.freezeCooldownPos", "crimsonIsle.ashfang.freezeCooldownPos")
    }

    private fun isEnabled() = AshfangManager.active && config.freezeCooldown
}
