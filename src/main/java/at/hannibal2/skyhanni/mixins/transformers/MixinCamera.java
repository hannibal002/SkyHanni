package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;

//? if >= 26.1 {
import at.hannibal2.skyhanni.events.minecraft.FovEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.injection.At;
//?}

@Mixin(Camera.class)
public class MixinCamera {
    //? if >= 26.1 {
    @ModifyExpressionValue(
        method = "calculateFov",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"
        )
    )
    private Object onBaseFov(Object original) {
        int baseFov = (Integer) original;
        FovEvent event = new FovEvent(baseFov);
        event.post();
        return event.getResult();
    }
    //?}
}
