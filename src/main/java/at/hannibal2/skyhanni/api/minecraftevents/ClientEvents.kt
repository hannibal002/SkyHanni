package at.hannibal2.hanni.api.minecraftevents

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.ActionBarData
import at.hannibal2.hanni.data.ChatManager
import at.hannibal2.hanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.hanni.events.minecraft.ResourcePackReloadEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.events.minecraft.WorldChangeEvent
import at.hannibal2.hanni.events.player.ClickAction
import at.hannibal2.hanni.events.player.PlayerInteractionEvent
import at.hannibal2.hanni.events.utils.PreInitFinishedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.IReloadableResourceManager
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent

@HanniModule
object ClientEvents {

    @HandleEvent
    fun onInitialize(event: PreInitFinishedEvent) {
        val minecraftResourceManager = Minecraft.getMinecraft().resourceManager

        if (minecraftResourceManager is IReloadableResourceManager) {
            minecraftResourceManager.registerReloadListener { resourceManager ->
                ResourcePackReloadEvent(resourceManager).post()
            }

            ResourcePackReloadEvent(minecraftResourceManager).post()
        }
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        ClientDisconnectEvent.post()
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        WorldChangeEvent().post()
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
        HanniTickEvent(totalTicks).post()
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
        PlayerInteractionEvent(ClickAction.fromForge(event.action), event.pos, event.face).post()
    }
}
