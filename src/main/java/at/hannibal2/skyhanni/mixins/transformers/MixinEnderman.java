package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.entity.EndermanTeleportEvent;
import net.minecraft.world.entity.monster.Enderman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//~ if < 26.3 'Enderman' -> 'EnderMan'
@Mixin(Enderman.class)
public abstract class MixinEnderman {
    @Inject(method = "teleport(DDD)Z", at = @At(value = "HEAD"), cancellable = true)
    private void onLivingUpdate(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (new EndermanTeleportEvent().post().isCancelled()) cir.setReturnValue(false);
    }
}
