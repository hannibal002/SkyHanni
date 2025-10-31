package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.BitsUpdateEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.SoundUtils
import at.hannibal2.hanni.utils.SoundUtils.createSound

@HanniModule
object NoBitsWarning {

    private val config get() = HanniMod.feature.misc.bits

    @HandleEvent
    fun onBitsGain(event: BitsUpdateEvent.BitsGain) {
        if (config.enableWarning && event.bitsAvailable == 0) {

            ChatUtils.clickableChat(
                "§bNo Bits Available! §eClick to buy booster cookies on the bazaar.",
                onClick = { HypixelCommands.bazaar("booster cookie") },
                hover = "§eClick to run /bz booster cookie!",
            )
            // TODO use reminder utils
            TitleManager.sendTitle("§bNo Bits Available")
            if (config.notificationSound) SoundUtils.repeatSound(100, 10, createSound("note.pling", 0.6f))
        }

        if (config.bitsGainChatMessage) {
            if (event.bits < config.messageThreshold) return
            ChatUtils.chat("You have gained §b${event.difference.addSeparators()} §eBits.")
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(35, "misc.noBitsWarning", "misc.noBitsWarning.enabled")
        event.move(40, "misc.noBitsWarning.enabled", "misc.bits.enableWarning")
        event.move(40, "misc.noBitsWarning.notificationSound", "misc.bits.notificationSound")
        event.move(94, "misc.bits.threshold", "misc.bits.messageThreshold")
    }
}
