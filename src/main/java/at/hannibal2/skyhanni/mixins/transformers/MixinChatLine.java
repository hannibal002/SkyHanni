package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.ChatLineData;
import at.hannibal2.skyhanni.mixins.hooks.GuiChatHook;
import net.minecraft.client.GuiMessage;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;
//? if > 1.21.11
//import net.minecraft.client.GuiMessageSource;

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
    //~ if > 1.21.11 'creationTick' -> 'addedTime'
    //~ if > 1.21.11 'Component line' -> 'Component content'
    //~ if > 1.21.11 'messageSignatureData' -> 'signature'
    //~ if > 1.21.11 'GuiMessageTag messageIndicator' -> 'GuiMessageSource source, GuiMessageTag tag'
    private void onInit(int creationTick, Component line, MessageSignature messageSignatureData, GuiMessageTag messageIndicator, CallbackInfo ci) {
        //~ if > 1.21.11 'component' -> 'hookComponent'
        Component component = GuiChatHook.getCurrentComponent();
        //~ if > 1.21.11 'component' -> 'hookComponent'
        //~ if > 1.21.11 'line' -> 'content'
        skyhanni$fullComponent = component == null ? line : component;
    }

}
