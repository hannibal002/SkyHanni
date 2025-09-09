package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {

    @Inject(method = "getDisplayName", at = @At(value = "RETURN"), cancellable = true)
    public void getDisplayName(CallbackInfoReturnable<Text> cir) {

        cir.setReturnValue(
            EntityData.getDisplayName((Entity) (Object) this, cir.getReturnValue())
        );
    }
}
