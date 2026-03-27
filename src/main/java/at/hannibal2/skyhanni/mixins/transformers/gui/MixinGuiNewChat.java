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
//? if >= 26.1
//import net.minecraft.client.GuiMessageSource;

@Mixin(ChatComponent.class)
public class MixinGuiNewChat {

    //? if < 26.1 {
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("HEAD"))
    private void setChatLine(Component chatComponent, MessageSignature signatureData, GuiMessageTag indicator, CallbackInfo ci) {
    //? } else {
    /*@Inject(method = "addMessage", at = @At("HEAD"))
    private void setChatLine(Component chatComponent, MessageSignature signatureData, GuiMessageSource source, GuiMessageTag indicator, CallbackInfo ci) {*/
    //? }
        GuiChatHook.setCurrentComponent(chatComponent);
    }
}
