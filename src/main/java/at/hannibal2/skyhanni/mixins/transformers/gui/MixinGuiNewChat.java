package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.mixins.hooks.GuiChatHook;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;

//? if >= 26.1
import net.minecraft.client.multiplayer.chat.GuiMessageSource;

@Mixin(ChatComponent.class)
public class MixinGuiNewChat {

    //~ if < 26.1 'addMessage' -> 'addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V'
    @Inject(method = "addMessage", at = @At("HEAD"))
    //~ if < 26.1 'GuiMessageSource source, GuiMessageTag indicator' -> 'GuiMessageTag indicator'
    private void setChatLine(Component chatComponent, MessageSignature signatureData, GuiMessageSource source, GuiMessageTag indicator, CallbackInfo ci) {
        GuiChatHook.setCurrentComponent(chatComponent);
    }
}
