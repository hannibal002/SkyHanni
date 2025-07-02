package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chat.ChatPeek;
import at.hannibal2.skyhanni.features.chroma.ChromaFontManagerKt;
import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ListIterator;

@Mixin(ChatHud.class)
public abstract class MixinChatHud {

    @Shadow
    public static int getHeight(double heightOption) {
        return 0;
    }

    @Shadow
    @Final
    private MinecraftClient client;

    @Redirect(method = "queueForRemoval", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;getTicks()I"), require = 0)
    private int clearChatHead(InGameHud instance) {
        return instance.getTicks() + 90;
    }

    @Redirect(method = "queueForRemoval", at = @At(value = "INVOKE", target = "Ljava/util/ListIterator;set(Ljava/lang/Object;)V"), require = 0)
    private <E> void clearChatTail(ListIterator instance, E e) {
        instance.remove();
    }

    @Inject(method = "getHeight()I", at = @At("HEAD"), cancellable = true)
    private void getHeight(CallbackInfoReturnable<Integer> cir) {
        if (ChatPeek.peek()) {
            cir.setReturnValue(getHeight(client.options.getChatHeightFocused().getValue()));
        }
    }

    @WrapMethod(
        method = "render"
    )
    private void wrapRender(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, Operation<Void> original) {
        ChromaFontManagerKt.setRenderingChat(true);
        ModifyVisualWords.INSTANCE.setChangeWords(false);

        original.call(context, currentTick, mouseX, mouseY, focused);

        ChromaFontManagerKt.setRenderingChat(false);
        ModifyVisualWords.INSTANCE.setChangeWords(true);
    }

}
