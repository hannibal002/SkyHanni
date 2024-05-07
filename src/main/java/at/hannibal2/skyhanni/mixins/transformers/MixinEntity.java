package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.NametagData;
import net.minecraft.entity.Entity;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {

    @Inject(method = "getDisplayName", at = @At("RETURN"))
    public void getDisplayName(CallbackInfoReturnable<IChatComponent> ci) {
        Entity entity = (Entity) (Object) this;
        NametagData.INSTANCE.getDisplayName(entity, ci.getReturnValue());
    }

}
