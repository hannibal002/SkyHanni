package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.mixins.hooks.GenericContainerScreenHook;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
//#if MC > 1.21.6
//$$ import at.hannibal2.skyhanni.data.GuiData;
//$$ import net.minecraft.client.gui.DrawContext;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

@Mixin(GenericContainerScreen.class)
abstract class MixinGenericContainerScreen {
    @Unique
    private final GenericContainerScreenHook skyhanni$hook = new GenericContainerScreenHook();

    @ModifyArg(
        method = "drawBackground",
        at = @At(
            value = "INVOKE",
            //#if MC < 1.21.6
            target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIII)V"
            //#else
            //$$ target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIII)V"
            //#endif
        ),
        index = 1
    )
    private Identifier getCustomTexture(Identifier sprite) {
        return skyhanni$hook.getTexture(sprite);
    }

    //#if MC > 1.21.6
    //$$ @Inject(method = "drawBackground", at = @At(value = "HEAD"), cancellable = true)
    //$$ private void cancelWardrobeBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY, CallbackInfo ci) {
    //$$     if (GuiData.INSTANCE.getPreDrawEventCancelled()) {
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //#endif
}
