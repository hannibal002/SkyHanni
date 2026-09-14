package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.fishing.LavaReplacement;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;

@Mixin(FluidStateModelSet.class)
public abstract class MixinFluidStateModelSet {
    @Inject(method = "bake", at = @At("RETURN"))
    private static void bake(CallbackInfoReturnable<Map<Fluid, FluidModel>> cir) {
        LavaReplacement.onModelsBaked(cir.getReturnValue());
    }

    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private FluidModel replaceLava(FluidModel original, FluidState state) {
        return LavaReplacement.getReplacementModel(state, original);
    }
}
