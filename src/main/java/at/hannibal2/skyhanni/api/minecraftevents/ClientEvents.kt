package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.data.ActionBarData
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.events.player.PlayerInteractionEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent

@SkyHanniModule
object ClientEvents {

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        ClientDisconnectEvent.post()
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        WorldChangeEvent.post()
    }

    var totalTicks = 0
        private set

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.START) return
        if (!MinecraftCompat.localPlayerExists) return
        if (!MinecraftCompat.localWorldExists) return

        DelayedRun.checkRuns()
        totalTicks++
        SkyHanniTickEvent(totalTicks).post()
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChatReceive(event: ClientChatReceivedEvent) {
        if (event.type.toInt() == 2) {
            ActionBarData.onChatReceive(event.message)?.let { result ->
                event.message = result
            }
        } else {
            val (result, cancel) = ChatManager.onChatReceive(event.message)

            result?.let {
                event.message = it
            }
            if (cancel) {
                event.isCanceled = true
            }
        }
    }

    @SubscribeEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        PlayerInteractionEvent(event.action, event.pos, event.face).post()
    }
}
