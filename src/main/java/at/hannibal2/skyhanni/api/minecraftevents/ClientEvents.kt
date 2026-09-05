package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.SkyHanniEvents
import at.hannibal2.skyhanni.api.event.SkyHanniEvents.DirtyReason
import at.hannibal2.skyhanni.data.ActionBarData
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.events.minecraft.ClientConnectEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.minecraft.ClientShutdownEvent
import at.hannibal2.skyhanni.events.minecraft.ResourcePackReloadEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils.skyhanniCreated
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.TextCompat.stripped
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.chat.GuiMessage
import net.minecraft.client.multiplayer.chat.GuiMessageSource
import net.minecraft.client.multiplayer.chat.GuiMessageTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackType
import java.util.concurrent.CompletableFuture

//? if < 26.2 {
/*import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEventLegacy
*///?}

@SkyHanniModule
object ClientEvents {
    var totalTicks = 0

    init {
        ClientTickEvents.END_CLIENT_TICK.register {
            if (!MinecraftCompat.localPlayerExists) return@register
            if (!MinecraftCompat.localWorldExists) return@register

            SkyHanniTickEvent(++totalTicks).post()
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            SkyHanniEvents.markEventCacheDirty(DirtyReason.SERVER_DISCONNECTED)
            ClientDisconnectEvent.post()
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            ClientConnectEvent.post()
        }

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            WorldChangeEvent.post()
        }

        LevelRenderEvents.COLLECT_SUBMITS.register { ctx ->
            SkyHanniRenderWorldEvent(
                ctx.poseStack(),
                ctx.levelState().cameraRenderState,
                ctx.submitNodeCollector(),
                Minecraft.getInstance().deltaTracker.getGameTimeDeltaPartialTick(true),
            ).post()
        }

        //? if < 26.2 {
        /*LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register { ctx ->
            SkyHanniRenderWorldEventLegacy(ctx.bufferSource()).post()
        }
        *///?}

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            Identifier.fromNamespaceAndPath("skyhanni", "resources"),
        ) { currentReload, _, preparationBarrier, reloadExecutor ->
            CompletableFuture.runAsync(
                { ResourcePackReloadEvent(currentReload.resourceManager()).post() },
                reloadExecutor,
            ).thenCompose(preparationBarrier::wait)
        }

        ClientReceiveMessageEvents.ALLOW_GAME.register(::onAllow)
        ClientReceiveMessageEvents.MODIFY_GAME.register(::onModify)
        ClientReceiveMessageEvents.GAME_CANCELED.register(::onCanceled)

        ClientLifecycleEvents.CLIENT_STOPPING.register { ClientShutdownEvent.post() }
    }

    var currentMessage: Component? = null

    private fun onAllow(message: Component, actionBar: Boolean): Boolean {
        // If we created the message we don't want to pipe it back into our events
        if (message.skyhanniCreated) return true

        if (actionBar) {
            // We never cancel the action bar
            return true
        }

        currentMessage = message

        val cancel = ChatManager.onChatAllow(message)

        if (cancel) {
            // The message doesn't get logged if we cancel it, so we do that ourselves
            val chatHudLine = GuiMessage(
                MinecraftCompat.hud.guiTicks,
                message,
                null,
                GuiMessageSource.SYSTEM_CLIENT,
                GuiMessageTag.system(),
            )
            MinecraftCompat.hud.chat.logChatMessage(chatHudLine)
        }

        // If we cancel then we don't allow the message
        return !cancel
    }

    private fun onModify(message: Component, actionBar: Boolean): Component {
        if (message.skyhanniCreated) return message

        if (actionBar) {
            // We don't have to worry about cancelling the action bar
            // This is more compatible with other mods changing the action bar as well
            // e.g. to remove HP/Mana
            val result = ActionBarData.onChatReceive(message)
            if (result == null) {
                return if (rainbowConfig()) {
                    TextHelper.createGradientText(
                        ColorUtils.getRandomColor(),
                        ColorUtils.getRandomColor(),
                        message.stripped,
                    )
                } else {
                    message
                }
            } else {
                return if (rainbowConfig()) {
                    TextHelper.createGradientText(
                        ColorUtils.getRandomColor(),
                        ColorUtils.getRandomColor(),
                        result.stripped,
                    )
                } else {
                    result
                }
            }
        }

        val new = ChatManager.onChatModify(message)
        // If new is null then we didn't want to change the message
        new?.let { return it }

        currentMessage?.let {
            if (it != message) ChatManager.onChatModifyOtherMod(it, message)
        }
        currentMessage = null

        return message
    }

    private fun onCanceled(message: Component, actionBar: Boolean) {
        if (actionBar) return
        if (currentMessage == message) {
            ChatManager.onChatCancel(message)
        }
        currentMessage = null
    }

    fun rainbowConfig() = SkyHanniMod.feature.misc.rainbowActionBar
}
