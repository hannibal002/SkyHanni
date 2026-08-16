package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Display.class)
public abstract class MixinDisplay {
    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Display;createFreshRenderState()Lnet/minecraft/world/entity/Display$RenderState;",
            shift = At.Shift.AFTER
        )
    )
    private void onFreshRenderStateCreated(CallbackInfo ci) {
        EntityData.onFreshDisplayRenderState((Display) (Object) this);
    }
}
