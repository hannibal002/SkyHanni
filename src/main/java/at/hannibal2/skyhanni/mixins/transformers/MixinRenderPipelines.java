package at.hannibal2.skyhanni.mixins.transformers;

//? if >= 26.2 {
import at.hannibal2.skyhanni.mixins.hooks.SkyHanniOutlineHook;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(RenderPipelines.class)
public abstract class MixinRenderPipelines {

    @Inject(method = "getStaticPipelines", at = @At("HEAD"))
    private static void registerSkyHanniOutlinePipelines(CallbackInfoReturnable<List<?>> cir) {
        SkyHanniOutlineHook.ensureCustomOutlinePipelinesRegistered();
    }
}
//?}
