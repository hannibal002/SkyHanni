package at.hannibal2.skyhanni.mixins.transformers;

//? if < 26.2 {
/*import at.hannibal2.skyhanni.mixins.hooks.GuiRendererHook;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public abstract class MixinRenderSystem {

    @Inject(method = "flipFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicUniforms;reset()V"))
    private static void clearChromaUniforms(CallbackInfo ci) {
        GuiRendererHook.clearChromaUniforms();
    }
}
*///?}
