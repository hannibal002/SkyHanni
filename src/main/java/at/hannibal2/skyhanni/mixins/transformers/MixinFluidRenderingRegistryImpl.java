//? if >= 26.1 {
package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.fishing.LavaReplacement;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.impl.client.rendering.fluid.FluidRenderingRegistryImpl;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = FluidRenderingRegistryImpl.class, remap = false)
public class MixinFluidRenderingRegistryImpl {

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void getButLava(Fluid fluid, CallbackInfoReturnable<FluidRenderHandler> cir) {
        if (LavaReplacement.isActive()) {
            if (fluid == Fluids.LAVA) cir.setReturnValue(FluidRenderingRegistryImpl.get(Fluids.WATER));
            else if (fluid == Fluids.FLOWING_LAVA) cir.setReturnValue(FluidRenderingRegistryImpl.get(Fluids.FLOWING_WATER));
        }
    }
}
//?}
