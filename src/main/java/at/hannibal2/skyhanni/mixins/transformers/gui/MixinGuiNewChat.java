package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.mixins.hooks.GuiChatHook;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;
//? if > 1.21.11
//import net.minecraft.client.GuiMessageSource;

@Mixin(ChatComponent.class)
public class MixinGuiNewChat {

    //~ if > 1.21.11 'addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V' -> 'addMessage'
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("HEAD"))
    //~ if > 1.21.11 'GuiMessageTag indicator' -> 'GuiMessageSource source, GuiMessageTag indicator'
    private void setChatLine(Component chatComponent, MessageSignature signatureData, GuiMessageTag indicator, CallbackInfo ci) {
        GuiChatHook.setCurrentComponent(chatComponent);
    }
}
