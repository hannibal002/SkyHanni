package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chroma.ChromaFontManagerKt;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ListIterator;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class MixinChatHud {

    @Redirect(method = "queueForRemoval", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;getTicks()I"), require = 0)
    private int clearChatHead(InGameHud instance) {
        return instance.getTicks() + 90;
    }

    @Redirect(method = "queueForRemoval", at = @At(value = "INVOKE", target = "Ljava/util/ListIterator;set(Ljava/lang/Object;)V"), require = 0)
    private <E> void clearChatTail(ListIterator instance, E e) {
        instance.remove();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(CallbackInfo info) {
        ChromaFontManagerKt.setRenderingChat(true);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender2(CallbackInfo info) {
        ChromaFontManagerKt.setRenderingChat(false);
    }

}
