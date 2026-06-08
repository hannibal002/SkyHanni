package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;

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
        return event.getResult();
    }
    //?}
}
