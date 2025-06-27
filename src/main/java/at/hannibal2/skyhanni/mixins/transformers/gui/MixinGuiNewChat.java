package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.features.chat.ChatPeek;
//#if TODO
import at.hannibal2.skyhanni.mixins.hooks.FontRendererHook;
//#endif
import at.hannibal2.skyhanni.mixins.hooks.GuiChatHook;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#if MC > 1.21
//$$ import net.minecraft.client.gui.DrawContext;
//$$ import net.minecraft.client.gui.hud.MessageIndicator;
//$$ import net.minecraft.network.message.MessageSignatureData;
//#endif

@Mixin(GuiNewChat.class)
public class MixinGuiNewChat {

    //#if MC < 1.21
    @Inject(method = "getChatOpen", at = @At("HEAD"), cancellable = true)
    public void onIsOpen(CallbackInfoReturnable<Boolean> cir) {
        if (ChatPeek.peek()) cir.setReturnValue(true);
    }
    //#endif

    @Inject(method =
        //#if MC < 1.21
        "setChatLine",
        //#else
        //$$ "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        //#endif
        at = @At("HEAD"))
    private void setChatLine(
        //#if MC < 1.21
        IChatComponent chatComponent, int chatLineId, int updateCounter, boolean displayOnly, CallbackInfo ci
        //#else
        //$$ Text chatComponent, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci
        //#endif
    ) {
        GuiChatHook.setCurrentComponent(chatComponent);
    }

    @Inject(method =
        //#if MC < 1.21
        "drawChat",
        //#else
        //$$ "render",
        //#endif
        at = @At(value = "INVOKE",
            //#if MC < 1.21
            target = "Lnet/minecraft/client/renderer/GlStateManager;enableBlend()V",
            //#else
            //$$ target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal = 1,
            //#endif
            shift = At.Shift.AFTER))
    private void setTextRenderIsFromChat(
        //#if MC < 1.21
        int updateCounter, CallbackInfo ci
        //#else
        //$$ DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci
        //#endif
    ) {
        //#if TODO
        FontRendererHook.INSTANCE.setCameFromChat(true);
        //#endif
    }

    @Inject(method =
        //#if MC < 1.21
        "drawChat",
        //#else
        //$$ "render",
        //#endif
        at = @At(value = "INVOKE",
            //#if MC < 1.21
            target = "Lnet/minecraft/client/renderer/GlStateManager;disableAlpha()V",
            //#else
            //$$ target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", ordinal = 0,
            //#endif
            shift = At.Shift.BEFORE))
    private void setTextRenderIsntFromChat(
        //#if MC < 1.21
        int updateCounter, CallbackInfo ci
        //#else
        //$$ DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci
        //#endif
    ) {
        //#if TODO
        FontRendererHook.INSTANCE.setCameFromChat(false);
        //#endif
    }
}
