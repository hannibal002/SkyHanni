package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object TransferCooldown {

    private val config get() = SkyHanniMod.feature.misc.commands

    private val delayedRunner: DelayedRun = DelayedRun
    var isActive: Boolean = false

    private var action: (() -> Unit)? = null

    @SubscribeEvent
    fun onWorldLoad(event: LorenzWorldChangeEvent) {
        if (!config.transferCooldown || isActive) return
        isActive = true
        delayedRunner.runDelayed(3.seconds) {
            if (config.transferCooldownMessage) ChatUtils.chat(
                "${EnumChatFormatting.GREEN}Player Transfer Cooldown has ended.",
                prefix = true
            )

            action?.invoke()
            action = null
            isActive = false
        }
    }

    fun transfer(command: () -> Unit) {
        if (isActive) action = { command.invoke() }
        else command.invoke()
    }
}
