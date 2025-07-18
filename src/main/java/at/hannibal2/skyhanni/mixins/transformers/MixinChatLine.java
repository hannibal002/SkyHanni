package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.ChatLineData;
import at.hannibal2.skyhanni.mixins.hooks.GuiChatHook;
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
    private IChatComponent skyHanni$fullComponent;

    @Unique
    @NotNull
    @Override
    public IChatComponent getSkyHanni_fullComponent() {
        return skyHanni$fullComponent;
    }

    @Unique
    @Override
    public void setSkyHanni_fullComponent(@NotNull IChatComponent fullComponent) {
        skyHanni$fullComponent = fullComponent;
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
        skyHanni$fullComponent = component == null ? line : component;
    }

}
