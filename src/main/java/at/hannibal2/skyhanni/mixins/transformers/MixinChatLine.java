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
//#if MC > 1.21
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;
//#endif

@Mixin(GuiMessage.class)
public class MixinChatLine implements ChatLineData {

    @Unique
    private Component skyHanni$fullComponent;

    @Unique
    @NotNull
    @Override
    public Component getSkyHanni_fullComponent() {
        return skyHanni$fullComponent;
    }

    @Unique
    @Override
    public void setSkyHanni_fullComponent(@NotNull Component fullComponent) {
        skyHanni$fullComponent = fullComponent;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(
        //#if MC < 1.21
        //$$ int updateCounterCreated, Text line, int chatLineID, CallbackInfo ci
        //#else
        int creationTick, Component line, MessageSignature messageSignatureData, GuiMessageTag messageIndicator, CallbackInfo ci
        //#endif
    ) {
        Component component = GuiChatHook.getCurrentComponent();
        skyHanni$fullComponent = component == null ? line : component;
    }

}
