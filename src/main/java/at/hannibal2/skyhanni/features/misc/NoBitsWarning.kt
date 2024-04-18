package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.BitsUpdateEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.createSound
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object NoBitsWarning {

    private val config get() = SkyHanniMod.feature.misc.bits

    fun sendBitsGainChatMessage(bits: Int) {
        if (!isChatMessageEnabled()) return
        if (bits < config.threshold) return
        ChatUtils.chat("You have gained §b${bits.addSeparators()} §eBits.")
    }

    @SubscribeEvent
    fun onBitsGain(event: BitsUpdateEvent.BitsGain) {
        if (!isWarningEnabled()) return
        if (event.bitsAvailable != 0) return

        ChatUtils.clickableChat(
            "§bNo Bits Available! §eClick to run /bz booster cookie.",
            "bz booster cookie"
        )
        LorenzUtils.sendTitle("§bNo Bits Available", 5.seconds)
        if (config.notificationSound) SoundUtils.repeatSound(100, 10, createSound("note.pling", 0.6f))
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(35, "misc.noBitsWarning", "misc.noBitsWarning.enabled")
        event.move(40, "misc.noBitsWarning.enabled", "misc.bits.enableWarning")
        event.move(40, "misc.noBitsWarning.notificationSound", "misc.bits.notificationSound")
    }

    private fun isChatMessageEnabled() = LorenzUtils.inSkyBlock && config.bitsGainChatMessage
    private fun isWarningEnabled() = LorenzUtils.inSkyBlock && config.enableWarning
}
