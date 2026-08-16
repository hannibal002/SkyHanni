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
            target = "Lnet/minecraft/world/entity/Display;updateRenderSubState(ZF)V",
            shift = At.Shift.AFTER
        )
    )
    private void onRenderSubStateUpdated(CallbackInfo ci) {
        EntityData.onDisplayRenderStateUpdate((Display) (Object) this);
    }
}
