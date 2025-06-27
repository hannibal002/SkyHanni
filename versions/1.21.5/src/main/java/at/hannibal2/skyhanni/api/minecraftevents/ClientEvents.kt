package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents

@SkyHanniModule
object ClientEvents {

    var totalTicks = 0

    init {

        // Tick event
        ClientTickEvents.START_WORLD_TICK.register(
            ClientTickEvents.StartWorldTick {
                if (!MinecraftCompat.localPlayerExists) return@StartWorldTick
                if (!MinecraftCompat.localWorldExists) return@StartWorldTick

                DelayedRun.checkRuns()
                totalTicks++
                SkyHanniTickEvent(totalTicks).post()
            },
        )

        // Disconnect event
        ClientPlayConnectionEvents.DISCONNECT.register(
            ClientPlayConnectionEvents.Disconnect { _, _ ->
                ClientDisconnectEvent.post()
            },
        )

        // World change event
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(
            ClientWorldEvents.AfterClientWorldChange { client, world ->
                WorldChangeEvent().post()
            },
        )
    }

}
