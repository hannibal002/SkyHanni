package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.minecraft.BaseFovEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public class MixinCamera {
    //? if >= 26.1 {
    @ModifyExpressionValue(
        method = "calculateFov",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
            ordinal = 0
        )
    )
    private Object onBaseFov(Object original) {
        int baseFov = (Integer) original;
        BaseFovEvent event = new BaseFovEvent(baseFov);
        event.post();
        float modified =
            (baseFov + event.getAdditive()) * event.getMultiplier();
        return (int) modified;
    }
    //?}
}
