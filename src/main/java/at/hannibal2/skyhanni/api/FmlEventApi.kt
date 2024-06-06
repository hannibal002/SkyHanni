package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent

@SkyHanniModule
object FmlEventApi {

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        ClientDisconnectEvent().postAndCatch()
    }

    // TODO when we have generic events, make this support generics
    @SubscribeEvent
    fun onEntityJoinWorld(event: EntityJoinWorldEvent) {
        EntityEnterWorldEvent(event.entity).postAndCatch()
    }
}
