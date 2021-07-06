package com.thatgravyboat.skyblockhud.tracker;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class KillTrackerHandler {

  public static final Set<UUID> attackedEntities = new HashSet<>();

  @SubscribeEvent
  public void onAttack(AttackEntityEvent event) {
    if (event.target != null) {
      attackedEntities.add(event.target.getUniqueID());
    }
  }

  @SubscribeEvent
  public void onDeath(LivingDeathEvent event) {
    if (false) {
      //Used for testing
      System.out.println(
        "----------------------------------------------------------------------------------------------------------------"
      );
      System.out.println("Name : " + event.entity.getName());
      System.out.println("UUID : " + event.entity.getUniqueID());
      NBTTagCompound tag = new NBTTagCompound();
      event.entity.writeToNBT(tag);
      System.out.println("Tag : " + tag);
      System.out.println("Damage : " + getDamageSourceString(event.source));
      System.out.println(
        "----------------------------------------------------------------------------------------------------------------"
      );
    }

    attackedEntities.remove(event.entity.getUniqueID());
  }

  public static String getDamageSourceString(DamageSource source) {
    return (
      "{ " +
      source.getDamageType() +
      ", " +
      source.isDamageAbsolute() +
      ", " +
      source.isDifficultyScaled() +
      ", " +
      source.isFireDamage() +
      ", " +
      source.isProjectile() +
      ", " +
      source.isUnblockable() +
      ", " +
      source.isExplosion() +
      ", " +
      source.isMagicDamage() +
      ", " +
      source.isCreativePlayer() +
      ", " +
      source.getSourceOfDamage() +
      " }"
    );
  }

  @SubscribeEvent
  public void onWorldChange(EntityJoinWorldEvent event) {
    if (event.entity != null) {
      if (
        event.entity
          .getUniqueID()
          .equals(Minecraft.getMinecraft().thePlayer.getUniqueID())
      ) {
        attackedEntities.clear();
      }
    }
  }
}
