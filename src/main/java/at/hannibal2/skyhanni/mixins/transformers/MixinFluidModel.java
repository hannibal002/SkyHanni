package at.hannibal2.skyhanni.mixins.transformers;

//? if >= 26.1 {
import at.hannibal2.skyhanni.mixins.hooks.FluidModelTransparencyOverride;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FluidModel.class)
public abstract class MixinFluidModel implements FluidModelTransparencyOverride {

    @Unique
    private @Nullable Transparency skyhanni$transparency = null;

    @Override
    public @Nullable Transparency skyhanni$getTransparency() {
        return skyhanni$transparency;
    }

    @Override
    public void skyhanni$setTransparency(@Nullable Transparency value) {
        skyhanni$transparency = value;
    }

    @ModifyReturnValue(method = "layer", at = @At("RETURN"))
    private ChunkSectionLayer overrideTransparency(ChunkSectionLayer original) {
        if (skyhanni$transparency != null) return ChunkSectionLayer.byTransparency(skyhanni$transparency);
        return original;
    }
}
//?}
