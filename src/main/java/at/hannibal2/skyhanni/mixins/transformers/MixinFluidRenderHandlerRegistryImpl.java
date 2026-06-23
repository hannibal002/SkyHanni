package at.hannibal2.skyhanni.mixins.transformers;

//? if < 26.1 {
/*import at.hannibal2.skyhanni.features.fishing.LavaReplacement;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.impl.client.rendering.fluid.FluidRenderHandlerRegistryImpl;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = FluidRenderHandlerRegistryImpl.class, remap = false)
public abstract class MixinFluidRenderHandlerRegistryImpl {

    @Shadow
    @Final
    private Map<Fluid, FluidRenderHandler> handlers;

    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private FluidRenderHandler replaceLava(FluidRenderHandler original, Fluid fluid) {
        Fluid replacementFluid = LavaReplacement.getReplacementFluid(fluid);
        return replacementFluid != fluid ? handlers.get(replacementFluid) : original;
    }
}
*///?}
