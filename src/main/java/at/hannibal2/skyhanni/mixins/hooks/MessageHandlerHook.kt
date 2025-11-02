package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.data.ActionBarData
import at.hannibal2.skyhanni.data.ChatManager
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.hud.ChatHudLine
import net.minecraft.client.gui.hud.MessageIndicator
import net.minecraft.text.Text

fun onGameMessage(message: Text, actionBar: Boolean, original: Operation<Void>) {
    if (actionBar) {
        ActionBarData.onChatReceive(message)?.let { result ->
            original.call(result, actionBar)
            return
        }
        original.call(message, actionBar)
        return
    }
    val (result, cancel) = ChatManager.onChatReceive(message)
    result?.let {
        // Will send both the unmodified and modified message into the Fabric Pipeline so other mods also get the old unmodified message
        // This sadly isn't preventable without switching fully to Fabric Chat Events
        // (which needs an event for cancelling and an event for modifying, which isn't a feasible split up with this code base size)
        ClientReceiveMessageEvents.ALLOW_GAME.invoker().allowReceiveGameMessage(message, actionBar)
        original.call(it, actionBar)
        return
    }
    if (cancel) {
        // We want to still log the message even if we cancel it
        val inGameHud = MinecraftClient.getInstance().inGameHud
        val chatHudLine = ChatHudLine(inGameHud.ticks, message, null, MessageIndicator.system())
        inGameHud.chatHud.logChatMessage(chatHudLine)

        // We also want to send the fabric canceled chat message event just to be nice
        ClientReceiveMessageEvents.ALLOW_GAME.invoker().allowReceiveGameMessage(message, actionBar)
        ClientReceiveMessageEvents.GAME_CANCELED.invoker().onReceiveGameMessageCanceled(message, actionBar)
        return
    }
    original.call(message, actionBar)
}
