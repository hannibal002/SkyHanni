package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.api.minecraftevents.EntityEvents;
import at.hannibal2.skyhanni.events.entity.EntityDeathEvent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    public void onDeath(DamageSource damageSource, CallbackInfo ci) {
        new EntityDeathEvent((LivingEntity) (Object) this).post();
    }

    @Inject(method = "applyDamage", at = @At("HEAD"))
    public void applyDamage(ServerWorld world, DamageSource source, float amount, CallbackInfo ci) {
        EntityEvents.postEntityHurt((LivingEntity) (Object) this, source, amount);
    }
}
