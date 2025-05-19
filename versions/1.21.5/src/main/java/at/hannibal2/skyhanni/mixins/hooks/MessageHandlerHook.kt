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
        }
        return
    }
    val (result, cancel) = ChatManager.onChatReceive(message)
    result?.let {
        original.call(it, actionBar)
        return
    }
    if (cancel) {
        // We want to still log the message even if we cancel it
        val inGameHud = MinecraftClient.getInstance().inGameHud
        val chatHudLine = ChatHudLine(inGameHud.ticks, message, null, MessageIndicator.system())
        inGameHud.chatHud.logChatMessage(chatHudLine)

        // We also want to send the fabric canceled chat message event just to be nice
        ClientReceiveMessageEvents.GAME.invoker().onReceiveGameMessage(message, actionBar)
        return
    }
    original.call(message, actionBar);
}
