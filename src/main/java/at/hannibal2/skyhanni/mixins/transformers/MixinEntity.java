package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @ModifyReturnValue(method = "getDisplayName", at = @At(value = "RETURN"))
    public Component modifyDisplayName(Component original) {
        return EntityData.getDisplayName((Entity) (Object) this, original);
    }
}
