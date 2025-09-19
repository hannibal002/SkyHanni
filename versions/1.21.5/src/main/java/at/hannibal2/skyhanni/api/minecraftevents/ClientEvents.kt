package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.minecraft.ResourcePackReloadEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceReloader
import net.minecraft.resource.ResourceType
import net.minecraft.util.Identifier
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

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

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
            object : IdentifiableResourceReloadListener {

                override fun getFabricId(): Identifier = Identifier.of("skyhanni", "resources")

                //#if MC < 1.21.9
                override fun reload(
                    synchronizer: ResourceReloader.Synchronizer,
                    manager: ResourceManager,
                    prepareExecutor: Executor,
                    applyExecutor: Executor,
                ): CompletableFuture<Void> {

                    return CompletableFuture.runAsync(
                        { ResourcePackReloadEvent(manager).post() },
                        applyExecutor,
                    ).thenCompose(synchronizer::whenPrepared)
                }
                //#else
                //$$ override fun reload(
                //$$     store: ResourceReloader.Store,
                //$$     prepareExecutor: Executor,
                //$$     reloadSynchronizer: ResourceReloader.Synchronizer,
                //$$     applyExecutor: Executor,
                //$$ ): CompletableFuture<Void> {
                //$$     return CompletableFuture.runAsync(
                //$$         { ResourcePackReloadEvent(store.resourceManager).post() },
                //$$         applyExecutor,
                //$$     ).thenCompose(reloadSynchronizer::whenPrepared)
                //$$ }
                //#endif
            },
        )

    }

}
