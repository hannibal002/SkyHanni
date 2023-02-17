package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.YawRotateEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow
    public float rotationYaw;

    @Shadow public abstract void setAngles(float yaw, float pitch);

    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
    public void skyhanniOnSetAngles(float yaw, float pitch, CallbackInfo ci) {
        Entity $this = (Entity) (Object) this;
        if (yaw != 0F && $this == Minecraft.getMinecraft().thePlayer) {
            YawRotateEvent yawRotateEvent = new YawRotateEvent(yaw, this.rotationYaw);
            yawRotateEvent.postAndCatch();
            if (yawRotateEvent.isCanceled()) {
                ci.cancel();
                this.setAngles(0, pitch);
            }
        }
    }
}
