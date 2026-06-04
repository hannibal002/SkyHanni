package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.mixins.hooks.GenericContainerScreenHook;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import at.hannibal2.skyhanni.data.GuiData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerScreen.class)
abstract class MixinGenericContainerScreen {
    @Unique
    private final GenericContainerScreenHook skyhanni$hook = new GenericContainerScreenHook();

    @ModifyArg(
        //~ if < 26.1 'extractBackground' -> 'renderBg'
        method = "extractBackground",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"
        ),
        index = 1
    )
    private Identifier getCustomTexture(Identifier sprite) {
        return skyhanni$hook.getTexture(sprite);
    }

    //~ if < 26.1 'extractBackground' -> 'renderBg'
    @Inject(method = "extractBackground", at = @At(value = "HEAD"), cancellable = true)
    //~ if < 26.1 'int mouseX, int mouseY, float a' -> 'float f, int i, int j'
    private void cancelWardrobeBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (GuiData.INSTANCE.getPreDrawEventCancelled()) {
            ci.cancel();
        }
    }
}
