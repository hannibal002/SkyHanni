package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.OptifineConnectedTexturesHookKt;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "net.optifine.ConnectedTextures", remap = false)
public class MixinOptifineConnectedTextures {

    @ModifyArg(method = "getConnectedTexture", at = @At(value = "INVOKE", target = "getConnectedTextureMultiPass"))
    private static BlockState modifyGetConnectedTextureMultiPass(BlockState state) {
        return OptifineConnectedTexturesHookKt.modifyConnectedTexturesBlockState(state);
    }

    @ModifyArg(method = "isNeighbour", at = @At(value = "INVOKE", target = "isNeighbour"), index = 4)
    private static BlockState modifyIsNeighbour(BlockState state) {
        return OptifineConnectedTexturesHookKt.modifyConnectedTexturesBlockState(state);
    }
}
