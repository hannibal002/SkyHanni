package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object TransferCooldown {

    private val config get() = SkyHanniMod.feature.misc.commands
    var isActive: Boolean = false
    private var action: (() -> Unit)? = null

    @SubscribeEvent
    fun onWorldLoad(event: LorenzWorldChangeEvent) {
        if (!config.transferCooldown || isActive) return
        isActive = true
        DelayedRun.runDelayed(3.seconds) {
            if (config.transferCooldownMessage && LorenzUtils.inSkyBlock) ChatUtils.chat(
                "§aPlayer Transfer Cooldown has ended."
            )

            action?.invoke()
            action = null
            isActive = false
        }
    }

    @SubscribeEvent
    fun onCommand(event: MessageSendToServerEvent) {
        if (!LorenzUtils.inSkyBlock || !config.transferCooldown || !isActive) return
        when (event.splitMessage[0]) {
            "/is" -> {
                event.isCanceled = true
                action = { HypixelCommands.island() }
            }

            "/warp" -> {
                event.isCanceled = true
                action = { HypixelCommands.warp(event.splitMessage.subList(1, event.splitMessage.size).joinToString(" ")) }
            }

            "/warpforge" -> {
                event.isCanceled = true
                action = { HypixelCommands.warp("forge") }
            }

            "/hub" -> {
                event.isCanceled = true
                action = { HypixelCommands.warp("hub") }
            }
        }
    }
}
