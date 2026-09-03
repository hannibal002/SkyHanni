package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.GuiData;
import at.hannibal2.skyhanni.mixins.hooks.GenericContainerScreenHook;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerScreen.class)
public abstract class MixinContainerScreen {
    @Unique
    private final GenericContainerScreenHook skyhanni$hook = new GenericContainerScreenHook();

    @ModifyArg(
        method = "extractBackground",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"
        ),
        index = 1
    )
    private Identifier getCustomTexture(Identifier sprite) {
        return skyhanni$hook.getTexture(sprite);
    }

    @Inject(method = "extractBackground", at = @At(value = "HEAD"), cancellable = true)
    private void cancelWardrobeBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (GuiData.getPreDrawEventCancelled()) {
            ci.cancel();
        }
    }
}
