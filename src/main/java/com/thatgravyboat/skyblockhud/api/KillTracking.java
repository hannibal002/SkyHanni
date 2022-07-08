//package com.thatgravyboat.skyblockhud.api;
//
//import com.thatgravyboat.skyblockhud.DevModeConstants;
//import com.thatgravyboat.skyblockhud.api.events.SkyBlockEntityKilled;
//import com.thatgravyboat.skyblockhud.api.sbentities.EntityTypeRegistry;
//import java.util.HashSet;
//import java.util.Set;
//import java.util.UUID;
//import net.minecraft.client.Minecraft;
//import net.minecraft.nbt.NBTTagCompound;
//import net.minecraft.util.DamageSource;
//import net.minecraftforge.common.MinecraftForge;
//import net.minecraftforge.event.entity.EntityJoinWorldEvent;
//import net.minecraftforge.event.entity.living.LivingDeathEvent;
//import net.minecraftforge.event.entity.player.AttackEntityEvent;
//import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//
//public class KillTracking {
//
//    public static final Set<UUID> attackedEntities = new HashSet<>();
//
//    @SubscribeEvent
//    public void onAttack(AttackEntityEvent event) {
//        if (event.target != null) {
//            attackedEntities.add(event.target.getUniqueID());
//        }
//    }
//
//    @SubscribeEvent
//    public void onDeath(LivingDeathEvent event) {
//        if (DevModeConstants.mobDeathLogging) {
//            //Used for testing
//            System.out.println("----------------------------------------------------------------------------------------------------------------");
//            System.out.println("Name : " + event.entity.getName());
//            System.out.println("UUID : " + event.entity.getUniqueID());
//            NBTTagCompound compound = new NBTTagCompound();
//            event.entity.writeToNBT(compound);
//            System.out.println("Tag : " + compound);
//            System.out.println("Damage : " + getDamageSourceString(event.source));
//            System.out.println("SBH Entity ID: " + EntityTypeRegistry.getEntityId(event.entity));
//            System.out.println("----------------------------------------------------------------------------------------------------------------");
//        }
//        if (attackedEntities.contains(event.entity.getUniqueID())) {
//            if (EntityTypeRegistry.getEntityId(event.entity) != null) {
//                MinecraftForge.EVENT_BUS.post(new SkyBlockEntityKilled(EntityTypeRegistry.getEntityId(event.entity), event.entity));
//            }
//            attackedEntities.remove(event.entity.getUniqueID());
//        }
//    }
//
//    public static String getDamageSourceString(DamageSource source) {
//        return ("{ " + source.getDamageType() + ", " + source.isDamageAbsolute() + ", " + source.isDifficultyScaled() + ", " + source.isFireDamage() + ", " + source.isProjectile() + ", " + source.isUnblockable() + ", " + source.isExplosion() + ", " + source.isMagicDamage() + ", " + source.isCreativePlayer() + ", " + source.getSourceOfDamage() + " }");
//    }
//
//    @SubscribeEvent
//    public void onWorldChange(EntityJoinWorldEvent event) {
//        if (event.entity == Minecraft.getMinecraft().thePlayer) {
//            attackedEntities.clear();
//        }
//    }
//}
