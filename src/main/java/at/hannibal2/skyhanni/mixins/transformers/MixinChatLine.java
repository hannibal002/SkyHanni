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
    private void onInit(int creationTick, Component line, MessageSignature messageSignatureData, GuiMessageTag messageIndicator, CallbackInfo ci) {
        Component component = GuiChatHook.getCurrentComponent();
        skyhanni$fullComponent = component == null ? line : component;
    }

}
