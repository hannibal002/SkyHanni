package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.ChatLineData;
import at.hannibal2.skyhanni.mixins.hooks.GuiChatHook;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;
//? if >= 26.1
import net.minecraft.client.multiplayer.chat.GuiMessageSource;

@Mixin(GuiMessage.class)
public class MixinChatLine implements ChatLineData {

    @Unique
    private Component skyhanni$fullComponent;

    @Unique
    @NotNull
    @Override
    public Component skyhanni$getFullComponent() {
        return skyhanni$fullComponent;
    }

    @Unique
    @Override
    public void skyhanni$setFullComponent(@NotNull Component fullComponent) {
        skyhanni$fullComponent = fullComponent;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    //~ if < 26.1 'addedTime' -> 'creationTick'
    //~ if < 26.1 'Component content' -> 'Component line'
    //~ if < 26.1 'signature' -> 'messageSignatureData'
    //~ if < 26.1 'GuiMessageSource source, GuiMessageTag tag' -> 'GuiMessageTag messageIndicator'
    private void onInit(int addedTime, Component content, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        //~ if < 26.1 'hookComponent' -> 'component'
        Component hookComponent = GuiChatHook.getCurrentComponent();
        //~ if < 26.1 'hookComponent' -> 'component'
        //~ if < 26.1 'content' -> 'line'
        skyhanni$fullComponent = hookComponent == null ? content : hookComponent;
    }

}
