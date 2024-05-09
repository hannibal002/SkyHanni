package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ChatUtils
import net.minecraft.client.Minecraft
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.Executors

object TransferCooldown {

    private val executor = Executors.newSingleThreadExecutor() // needed because in some instances the "WorldEvent.Load" gets called multiple times

    @Volatile var isTaskScheduled = false
        private set

    private val transferCooldownEndedEvent = mutableListOf<() -> Unit>()
    private val mc get() = Minecraft.getMinecraft()
    private val config get() = SkyHanniMod.feature.misc.commands

    fun registerListener(listener: () -> Unit) {
        transferCooldownEndedEvent.add(listener)
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        mc.theWorld ?: return
        mc.thePlayer ?: return
        mc.thePlayer.inventory ?: return
        if (!config.transferCooldown) return
        synchronized(this) {
            if (!isTaskScheduled) {
                executor.submit {
                    isTaskScheduled = true
                    Thread.sleep(3000L)
                    if (config.transferCooldownMessage) {
                        ChatUtils.chat("${EnumChatFormatting.GREEN}Player Transfer Cooldown has ended.", prefix = true)
                    }
                    transferCooldownEndedEvent.forEach {
                        it()
                    }
                    transferCooldownEndedEvent.clear()
                    isTaskScheduled = false
                }
            }
        }
    }
}
