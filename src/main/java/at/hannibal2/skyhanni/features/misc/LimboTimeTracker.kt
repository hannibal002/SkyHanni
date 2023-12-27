package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.features.commands.LimboCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class LimboTimeTracker {
    private val config get() = SkyHanniMod.feature.misc

    private var limboJoinTime = SimpleTimeMark.farPast()
    private var shownPB = false
    private var oldPB: Duration = 0.seconds
    private var userLuck: Double = 0.0
    private val userLuckMultiplier = 0.000810185

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (event.message == "§cYou are AFK. Move around to return from AFK." || event.message == "§cYou were spawned in Limbo.") {
            limboJoinTime = SimpleTimeMark.now()
        }
    }

    @SubscribeEvent
    fun catchPlaytime(event: MessageSendToServerEvent) {
        if (event.message.startsWith("/playtime") && LimboCommands.inLimbo) {
            event.isCanceled
            LimboCommands.printPlaytime()
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (LimboCommands.inLimbo && !shownPB && limboJoinTime.passedSince() >= config.limboTimePB.seconds && config.limboTimePB != 0) {
            shownPB = true
            oldPB = config.limboTimePB.seconds
            LorenzUtils.chat("§d§lPERSONAL BEST§f! You've surpassed your previous record of §e$oldPB§f!")
            LorenzUtils.chat("§fKeep it up!")
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (!LimboCommands.inLimbo) return
        leaveLimbo()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!LimboCommands.inLimbo) return

        if (LorenzUtils.inSkyBlock) {
            leaveLimbo()
            return
        }

        val duration = limboJoinTime.passedSince().format()
        config.showTimeInLimboPosition.renderString("§eIn limbo since §b$duration", posLabel = "Limbo Time Tracker")
    }

    private fun leaveLimbo() {
        LimboCommands.inLimbo = false
        if (!isEnabled()) return
        val passedSince = limboJoinTime.passedSince()
        val duration = passedSince.format()
        val currentPB = config.limboTimePB.seconds
        if (passedSince > currentPB) {
            oldPB = currentPB
            config.limboTimePB = passedSince.toInt(DurationUnit.SECONDS)
            LorenzUtils.chat("§fYou were AFK in Limbo for §e$duration§f! §d§lPERSONAL BEST§r§f!")
            LorenzUtils.chat("§fYour previous Personal Best was §e$oldPB.")
            userLuck = config.limboTimePB * userLuckMultiplier
            LorenzUtils.chat("§fYour §aPersonal Bests§f perk is now granting you §a+${userLuck.round(2)}✴ SkyHanni User Luck§f!")
        } else LorenzUtils.chat("§fYou were AFK in Limbo for §e$duration§f.")
        shownPB = false
    }

    fun isEnabled() = config.showTimeInLimbo
}
