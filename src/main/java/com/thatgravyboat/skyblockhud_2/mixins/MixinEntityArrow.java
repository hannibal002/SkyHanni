//package com.thatgravyboat.skyblockhud.mixins;
//
//import com.thatgravyboat.skyblockhud.api.KillTracking;
//import net.minecraft.client.Minecraft;
//import net.minecraft.entity.Entity;
//import net.minecraft.entity.projectile.EntityArrow;
//import net.minecraft.util.MovingObjectPosition;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.ModifyVariable;
//
//@Mixin(EntityArrow.class)
//public class MixinEntityArrow {
//
//    @Shadow
//    public Entity shootingEntity;
//
//    @ModifyVariable(method = "onUpdate", at = @At(value = "STORE", ordinal = 1))
//    public MovingObjectPosition onUpdate(MovingObjectPosition position) {
//        if (position != null && position.entityHit != null && this.shootingEntity != null && this.shootingEntity.getUniqueID().equals(Minecraft.getMinecraft().thePlayer.getUniqueID())) {
//            KillTracking.attackedEntities.add(position.entityHit.getUniqueID());
//        }
//        return position;
//    }
//}
