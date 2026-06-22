package at.hannibal2.skyhanni.mixins.transformers;

//? if >= 26.1 {
import at.hannibal2.skyhanni.features.fishing.LavaReplacement;
import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(FluidStateModelSet.class)
public abstract class MixinFluidStateModelSet {

    @ModifyReturnValue(method = "bake", at = @At("RETURN"))
    private static Map<Fluid, FluidModel> bake(Map<Fluid, FluidModel> original, MaterialBaker materials) {
        FluidModel opaqueWaterModel = LavaReplacement.OPAQUE_WATER_MODEL.bake(materials, () -> "Opaque Water");
        opaqueWaterModel.skyhanni$setTransparency(Transparency.NONE);

        return new ImmutableMap.Builder<Fluid, FluidModel>()
            .putAll(original)
            .put(LavaReplacement.OPAQUE_WATER, opaqueWaterModel)
            .put(LavaReplacement.OPAQUE_FLOWING_WATER, opaqueWaterModel)
            .build();
    }

    @WrapOperation(
        method = "get",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;getType()Lnet/minecraft/world/level/material/Fluid;"
        )
    )
    private Fluid replaceLava(FluidState instance, Operation<Fluid> original) {
        Fluid orig = original.call(instance);
        if (LavaReplacement.isActive()) {
            if (orig == Fluids.LAVA) return LavaReplacement.OPAQUE_WATER;
            if (orig == Fluids.FLOWING_LAVA) return LavaReplacement.OPAQUE_FLOWING_WATER;
        }
        return orig;
    }
}
//?}
