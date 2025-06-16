package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import at.hannibal2.skyhanni.events.entity.EntityRemovedEvent;
import net.minecraft.entity.Entity;
import net.minecraft.world.entity.EntityLike;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.entity.ClientEntityManager.Listener")
public class MixinClientEntityManager<T extends EntityLike> {

    @Shadow
    @Final
    private T entity;

    @Inject(method = "remove", at = @At("HEAD"))
    private void remove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (entity instanceof Entity entity) {
            EntityData.despawnEntity(entity);
            new EntityRemovedEvent(entity).post();
        }
    }
}
