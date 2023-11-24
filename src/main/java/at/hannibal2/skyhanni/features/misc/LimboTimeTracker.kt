package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.DurationUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


class LimboTimeTracker {
    private val config get() = SkyHanniMod.feature.misc

    private var limboJoinTime = SimpleTimeMark.farPast()
    private var inLimbo = false

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (event.message == "§cYou are AFK. Move around to return from AFK." || event.message == "§cYou were spawned in Limbo.") {
            limboJoinTime = SimpleTimeMark.now()
            inLimbo = true
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (!inLimbo) return
        leaveLimbo()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!inLimbo) return

        if (LorenzUtils.inSkyBlock) {
            leaveLimbo()
            return
        }

        val duration = limboJoinTime.passedSince().format()
        config.showTimeInLimboPosition.renderString("§eIn limbo since §b$duration", posLabel = "Limbo Time Tracker")
    }

    private fun leaveLimbo() {
        inLimbo = false
        if (!isEnabled()) return
        val passedSince = limboJoinTime.passedSince()
        val duration = passedSince.format()
        if (passedSince.toInt(DurationUnit.SECONDS) > config.limboTimePB ) {
            val oldPB: Duration = config.limboTimePB.seconds
            LorenzUtils.chat("§fYou were AFK in Limbo for §e$duration§f! §d§lPERSONAL BEST§r§f!")
            LorenzUtils.chat("§fYour previous Personal Best was §e$oldPB.")
            LorenzUtils.chat("§fYour §aPersonal Bests§f perk is now granting you §a+00.00 ✴ SkyHanni User Luck§f!")
            config.limboTimePB = passedSince.toInt(DurationUnit.SECONDS)
        } else LorenzUtils.chat("§fYou were AFK in Limbo for §e$duration§f.")
    }

    fun isEnabled() = config.showTimeInLimbo
}
