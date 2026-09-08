package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TransientEntitySectionManager.Callback.class)
public abstract class MixinTransientEntitySectionManager$Callback<T extends EntityAccess> {
    @Shadow
    @Final
    private T entity;

    @Inject(method = "onRemove", at = @At("HEAD"))
    private void remove(RemovalReason reason, CallbackInfo ci) {
        if (entity instanceof Entity typedEntity) {
            EntityData.despawnEntity(typedEntity);
        }
    }
}
