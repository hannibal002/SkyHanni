package at.hannibal2.hanni.mixins.transformers;

import at.hannibal2.hanni.mixins.hooks.ChatLineData;
import at.hannibal2.hanni.mixins.hooks.GuiChatHook;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.util.IChatComponent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC > 1.21
//$$ import net.minecraft.client.gui.hud.MessageIndicator;
//$$ import net.minecraft.network.message.MessageSignatureData;
//#endif

@Mixin(ChatLine.class)
public class MixinChatLine implements ChatLineData {

    @Unique
    private IChatComponent hanni$fullComponent;

    @Unique
    @NotNull
    @Override
    public IChatComponent getHanni_fullComponent() {
        return hanni$fullComponent;
    }

    @Unique
    @Override
    public void setHanni_fullComponent(@NotNull IChatComponent fullComponent) {
        hanni$fullComponent = fullComponent;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(
        //#if MC < 1.21
        int updateCounterCreated, IChatComponent line, int chatLineID, CallbackInfo ci
        //#else
        //$$ int creationTick, Text line, MessageSignatureData messageSignatureData, MessageIndicator messageIndicator, CallbackInfo ci
        //#endif
    ) {
        IChatComponent component = GuiChatHook.getCurrentComponent();
        hanni$fullComponent = component == null ? line : component;
    }

}
