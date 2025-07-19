package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.GuiRendererHook;
import at.hannibal2.skyhanni.utils.render.RoundedShapeDrawer;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {

    @Inject(method = "flipFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/DynamicUniforms;clear()V"))
    private static void clearChromaUniforms(CallbackInfo ci) {
        GuiRendererHook.INSTANCE.getChromaUniform().clear();
        RoundedShapeDrawer.INSTANCE.clearUniforms();
    }

}
