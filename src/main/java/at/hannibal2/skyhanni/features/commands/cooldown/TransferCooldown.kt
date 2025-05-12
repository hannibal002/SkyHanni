package at.hannibal2.skyhanni.features.commands.cooldown

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TransferCooldown {
    private val config get() = SkyHanniMod.feature.misc.commands
    private var lastRunCompleted: SimpleTimeMark = SimpleTimeMark.farPast()
    private var action: (() -> Unit)? = null

    @HandleEvent
    fun onWorldChange() {
        if (!config.transferCooldown || lastRunCompleted.isInFuture()) return
        lastRunCompleted = DelayedRun.runDelayed(3.seconds) {
            action?.invoke()
            action = null
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onCommand(event: MessageSendToServerEvent) {
        if (!config.transferCooldown || lastRunCompleted.isInPast()) return
        action = when (event.splitMessage[0]) {
            "/is" -> HypixelCommands::island
            "/warp" -> { ->
                val warpDestination = event.splitMessage.subList(1, event.splitMessage.size).joinToString(" ")
                HypixelCommands.warp(warpDestination)
            }
            "/warpforge" -> { -> HypixelCommands.warp("forge") }
            "/hub" -> { ->  HypixelCommands.warp("hub") }
            else -> null
        }
        if (action != null) event.cancel()
    }
}
