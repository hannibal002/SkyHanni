package at.hannibal2.hanni.features.commands

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.MessageSendToServerEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import kotlin.time.Duration.Companion.seconds

@HanniModule
object TransferCooldown {

    private val config get() = HanniMod.feature.misc.commands
    private var lastRunCompleted: SimpleTimeMark = SimpleTimeMark.farPast()
    private var action: (() -> Unit)? = null

    @HandleEvent
    fun onWorldChange() {
        if (!config.transferCooldown || lastRunCompleted.isInFuture()) return
        lastRunCompleted = DelayedRun.runDelayed(3.seconds) {
            if (config.transferCooldownMessage && SkyBlockUtils.inSkyBlock) {
                ChatUtils.chat("§aPlayer Transfer Cooldown has ended.")
            }
            action?.invoke()
            action = null
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onCommand(event: MessageSendToServerEvent) {
        if (!config.transferCooldown || lastRunCompleted.isInPast()) return
        when (event.splitMessage[0]) {
            "/is" -> {
                event.cancel()
                action = { HypixelCommands.island() }
            }

            "/warp" -> {
                event.cancel()
                action = {
                    HypixelCommands.warp(event.splitMessage.subList(1, event.splitMessage.size).joinToString(" "))
                }
            }

            "/warpforge" -> {
                event.cancel()
                action = { HypixelCommands.warp("forge") }
            }

            "/hub" -> {
                event.cancel()
                action = { HypixelCommands.warp("hub") }
            }
        }
    }
}
