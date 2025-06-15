package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.entity.EndermanTeleportEvent;
import net.minecraft.entity.mob.EndermanEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndermanEntity.class)
public class MixinEntityEnderman {

    @Inject(method = "teleportTo(DDD)Z", at = @At(value = "HEAD"), cancellable = true)
    private void onLivingUpdate(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (new EndermanTeleportEvent().post()) cir.setReturnValue(false);
    }
}
