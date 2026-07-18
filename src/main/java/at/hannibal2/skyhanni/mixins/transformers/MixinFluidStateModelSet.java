package at.hannibal2.skyhanni.mixins.transformers;

//? if >= 26.1 {
import at.hannibal2.skyhanni.features.fishing.LavaReplacement;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(FluidStateModelSet.class)
public abstract class MixinFluidStateModelSet {

    @ModifyReturnValue(method = "bake", at = @At("RETURN"))
    private static Map<Fluid, FluidModel> bake(Map<Fluid, FluidModel> original, MaterialBaker materials) {
        return LavaReplacement.addOpaqueWaterModel(original, materials);
    }

    @WrapOperation(
        method = "get",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;getType()Lnet/minecraft/world/level/material/Fluid;"
        )
    )
    private Fluid replaceLava(FluidState instance, Operation<Fluid> original) {
        return LavaReplacement.getReplacementFluid(original.call(instance));
    }
}
//?}
