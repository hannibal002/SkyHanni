package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ActionBarData
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.minecraft.ResourcePackReloadEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.client.GuiMessage
import net.minecraft.client.GuiMessageTag
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
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

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            object : IdentifiableResourceReloadListener {

                override fun getFabricId(): ResourceLocation = ResourceLocation.fromNamespaceAndPath("skyhanni", "resources")

                //? < 1.21.9 {
                @Suppress("ForbiddenVoid")
                override fun reload(
                    synchronizer: PreparableReloadListener.PreparationBarrier,
                    manager: ResourceManager,
                    prepareExecutor: Executor,
                    applyExecutor: Executor,
                ): CompletableFuture<Void> {

                    return CompletableFuture.runAsync(
                        { ResourcePackReloadEvent(manager).post() },
                        applyExecutor,
                    ).thenCompose(synchronizer::wait)
                }
                //?} else {
                 /*override fun reload(
                     store: PreparableReloadListener.SharedState,
                     prepareExecutor: Executor,
                     reloadSynchronizer: PreparableReloadListener.PreparationBarrier,
                     applyExecutor: Executor,
                 ): CompletableFuture<Void> {
                     return CompletableFuture.runAsync(
                         { ResourcePackReloadEvent(store.resourceManager()).post() },
                         applyExecutor,
                     ).thenCompose(reloadSynchronizer::wait)
                 }
                *///?}
            },
        )

        ClientReceiveMessageEvents.ALLOW_GAME.register(::onAllow)
        ClientReceiveMessageEvents.MODIFY_GAME.register(::onModify)

    }

    private var lastMessage: Component? = null
    private var lastResult: Component? = null

    private fun onAllow(message: Component, actionBar: Boolean): Boolean {
        lastMessage = message
        if (actionBar) {
            // we never cancel the action bar
            return true
        }

        val (result, cancel) = ChatManager.onChatReceive(message)
        lastResult = result

        if (cancel) {
            // the message doesn't get logged if we cancel it, so we do that ourselves
            val inGameHud = Minecraft.getInstance().gui
            val chatHudLine = GuiMessage(inGameHud.guiTicks, message, null, GuiMessageTag.system())
            inGameHud.chat.logChatMessage(chatHudLine)
        }

        // if we cancel then we don't allow the message
        return !cancel
    }

    private fun onModify(message: Component, actionBar: Boolean): Component {
        // we check if the message is the same as the one from allow
        // if someone else modifies the message it won't be the same but what can you do about that
        if (lastMessage == message && !actionBar) {
            // if last result is null then we didn't want to change the message
            lastResult?.let { return it }
        } else if (actionBar) {
            // we don't have to worry about cancelling the action bar
            // this is more compatible with other mods changing the action bar as well
            // ie to remove hp/mana
            val result = ActionBarData.onChatReceive(message)
            if (result == null) {
                return if (rainbowConfig()) {
                    TextHelper.createGradientText(
                        ColorUtils.getRandomColor(),
                        ColorUtils.getRandomColor(),
                        message.string.removeColor()
                    )
                } else {
                    message
                }
            } else {
                return if (rainbowConfig()) {
                    TextHelper.createGradientText(
                        ColorUtils.getRandomColor(),
                        ColorUtils.getRandomColor(),
                        result.string.removeColor()
                    )
                } else {
                    result
                }
            }
        }

        return message
    }

    fun rainbowConfig() = SkyHanniMod.feature.misc.rainbowActionBar

}
