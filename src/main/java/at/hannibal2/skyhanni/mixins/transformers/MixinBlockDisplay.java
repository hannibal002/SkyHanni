package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Display.BlockDisplay.class)
public abstract class MixinBlockDisplay {
    @ModifyArg(
        method = "updateRenderSubState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Display$BlockDisplay$BlockRenderState;<init>(Lnet/minecraft/world/level/block/state/BlockState;)V"
        ),
        index = 0
    )
    private BlockState onBlockRenderStateCreated(BlockState state) {
        EntityData.onBlockDisplayRenderState((Display.BlockDisplay) (Object) this, state);
        return state;
    }
}
