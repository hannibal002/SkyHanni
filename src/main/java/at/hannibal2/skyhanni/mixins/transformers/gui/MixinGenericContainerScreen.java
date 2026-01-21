package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.mixins.hooks.GenericContainerScreenHook;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
//? > 1.21.6 {
/*import at.hannibal2.skyhanni.data.GuiData;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

@Mixin(ContainerScreen.class)
abstract class MixinGenericContainerScreen {
    @Unique
    private final GenericContainerScreenHook skyhanni$hook = new GenericContainerScreenHook();

    @ModifyArg(
        method = "renderBg",
        at = @At(
            value = "INVOKE",
            //? < 1.21.6 {
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"
            //?} else {
            /*target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"
            *///?}
        ),
        index = 1
    )
    private ResourceLocation getCustomTexture(ResourceLocation sprite) {
        return skyhanni$hook.getTexture(sprite);
    }

    //? > 1.21.6 {
    /*@Inject(method = "renderBg", at = @At(value = "HEAD"), cancellable = true)
    private void cancelWardrobeBackground(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        if (GuiData.INSTANCE.getPreDrawEventCancelled()) {
            ci.cancel();
        }
    }
    *///?}
}
