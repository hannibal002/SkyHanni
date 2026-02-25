package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chat.ChatPeek;
import at.hannibal2.skyhanni.features.chroma.ChromaFontManagerKt;
import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ListIterator;

@Mixin(ChatComponent.class)
public abstract class MixinChatHud {

    @Shadow
    public static int getHeight(double heightOption) {
        return 0;
    }

    @Shadow
    @Final
    private Minecraft minecraft;

    @Redirect(method = "deleteMessageOrDelay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;getGuiTicks()I"), require = 0)
    private int clearChatHead(Gui instance) {
        return instance.getGuiTicks() + 90;
    }

    @Redirect(method = "deleteMessageOrDelay", at = @At(value = "INVOKE", target = "Ljava/util/ListIterator;set(Ljava/lang/Object;)V"), require = 0)
    private <E> void clearChatTail(ListIterator instance, E e) {
        instance.remove();
    }

    @Inject(method = "getHeight()I", at = @At("HEAD"), cancellable = true)
    private void getHeight(CallbackInfoReturnable<Integer> cir) {
        if (ChatPeek.peek()) {
            cir.setReturnValue(getHeight(this.minecraft.options.chatHeightFocused().get()));
        }
    }

    @WrapMethod(
            //? if < 1.21.11 {
            method = "render"
            //?} else
            //method = "render(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IIZ)V"
    )

            //? if < 1.21.11 {
    private void wrapRender(GuiGraphics context, int currentTick, int mouseX, int mouseY, boolean focused, Operation<Void> original) {
        //?} else
        //private void wrapRender(ChatComponent.ChatGraphicsAccess chatGraphicsAccess, int i, int j, boolean bl, Operation<Void> original) {
        ChromaFontManagerKt.setRenderingChat(true);
        ModifyVisualWords.INSTANCE.setChangeWords(false);

        //? if < 1.21.11 {
        original.call(context, currentTick, mouseX, mouseY, focused);
        //?} else
        //original.call(chatGraphicsAccess, i, j, bl);

        ChromaFontManagerKt.setRenderingChat(false);
        ModifyVisualWords.INSTANCE.setChangeWords(true);
    }

}
