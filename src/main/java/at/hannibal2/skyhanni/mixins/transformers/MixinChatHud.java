package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chat.ChatPeek;
import at.hannibal2.skyhanni.features.chroma.ChromaFontManagerKt;
import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatComponent.class)
public abstract class MixinChatHud {

    @Shadow
    public static int getHeight(double heightOption) {
        return 0;
    }

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "getHeight()I", at = @At("HEAD"), cancellable = true)
    private void getHeight(CallbackInfoReturnable<Integer> cir) {
        if (ChatPeek.peek()) {
            cir.setReturnValue(getHeight(this.minecraft.options.chatHeightFocused().get()));
        }
    }

    //? if >= 26.1 {
    @WrapMethod(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V")
    //?} else {
    /*@WrapMethod(method = "render(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IIZ)V")
    *///?}
    private void wrapRender(
        ChatComponent.ChatGraphicsAccess chatGraphicsAccess,
        int screenHeight,
        int ticks,
        //~ if < 26.1 'ChatComponent.DisplayMode' -> 'boolean'
        ChatComponent.DisplayMode displayMode,
        Operation<Void> original
    ) {
        ChromaFontManagerKt.setRenderingChat(true);
        ModifyVisualWords.INSTANCE.setChangeWords(false);

        original.call(
            chatGraphicsAccess,
            screenHeight,
            ticks,
            displayMode
        );

        ChromaFontManagerKt.setRenderingChat(false);
        ModifyVisualWords.INSTANCE.setChangeWords(true);
    }
}
