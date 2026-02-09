package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.entity.EntityDeathEvent;
import at.hannibal2.skyhanni.events.entity.EntityEquipmentChangeEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "die", at = @At("HEAD"))
    public void onDeath(DamageSource damageSource, CallbackInfo ci) {
        new EntityDeathEvent((LivingEntity) (Object) this).post();
    }

    @Inject(method = "setItemSlot", at = @At("TAIL"))
    public void setItemStack(EquipmentSlot equipment, ItemStack itemStack, CallbackInfo ci) {
        new EntityEquipmentChangeEvent((Entity) (Object) this, equipment.getId(), itemStack).post();
    }
}
