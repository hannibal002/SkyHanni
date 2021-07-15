package com.thatgravyboat.skyblockhud.api.sbentities;

import com.thatgravyboat.skyblockhud.location.LocationHandler;
import com.thatgravyboat.skyblockhud.location.Locations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;

public class EntityTypeHelper {

    public static boolean isZealot(Entity entity) {
        if (entity instanceof EntityEnderman) {
            EntityEnderman enderman = ((EntityEnderman) entity);
            double maxHealthBase = enderman.getAttributeMap().getAttributeInstanceByName("generic.maxHealth").getBaseValue();
            if (maxHealthBase == 13000 || (maxHealthBase == 2000d && enderman.getHeldBlockState().getBlock().equals(Blocks.end_portal_frame))) {
                return LocationHandler.getCurrentLocation().equals(Locations.DRAGONSNEST);
            }
        }
        return false;
    }

    public static boolean isCrypt(Entity entity){
        if (entity instanceof EntityZombie) {
            EntityZombie zombie = ((EntityZombie) entity);
            double maxHealthBase = zombie.getAttributeMap().getAttributeInstanceByName("generic.maxHealth").getBaseValue();
            if (maxHealthBase != 2000d) return false;
            if (zombie.getEquipmentInSlot(0) == null || !zombie.getEquipmentInSlot(0).getItem().equals(Items.iron_sword))
                return false;
            if (zombie.getEquipmentInSlot(1) == null || !zombie.getEquipmentInSlot(1).getItem().equals(Items.chainmail_boots))
                return false;
            if (zombie.getEquipmentInSlot(2) == null || !zombie.getEquipmentInSlot(2).getItem().equals(Items.chainmail_leggings))
                return false;
            return zombie.getEquipmentInSlot(3) != null && zombie.getEquipmentInSlot(3).getItem().equals(Items.chainmail_chestplate);
        }
        return false;
    }
}
