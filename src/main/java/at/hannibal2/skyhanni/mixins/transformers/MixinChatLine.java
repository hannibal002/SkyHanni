package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.ChatLineData;
import at.hannibal2.skyhanni.mixins.hooks.GuiChatHook;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC > 1.21
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
//#endif

@Mixin(ChatHudLine.class)
public class MixinChatLine implements ChatLineData {

    @Unique
    private Text skyHanni$fullComponent;

    @Unique
    @NotNull
    @Override
    public Text getSkyHanni_fullComponent() {
        return skyHanni$fullComponent;
    }

    @Unique
    @Override
    public void setSkyHanni_fullComponent(@NotNull Text fullComponent) {
        skyHanni$fullComponent = fullComponent;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(
        //#if MC < 1.21
        //$$ int updateCounterCreated, Text line, int chatLineID, CallbackInfo ci
        //#else
        int creationTick, Text line, MessageSignatureData messageSignatureData, MessageIndicator messageIndicator, CallbackInfo ci
        //#endif
    ) {
        Text component = GuiChatHook.getCurrentComponent();
        skyHanni$fullComponent = component == null ? line : component;
    }

}
