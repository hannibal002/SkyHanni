package com.thatgravyboat.skyblockhud.handlers.sbentities;

import com.thatgravyboat.skyblockhud.location.LocationHandler;
import com.thatgravyboat.skyblockhud.location.Locations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.init.Blocks;

public class EntityTypeHelper {

    public static boolean isZealot(Entity entity) {
        if (entity instanceof EntityEnderman) {
            EntityEnderman enderman = ((EntityEnderman) entity);
            double maxHealthBase = enderman
                .getAttributeMap()
                .getAttributeInstanceByName("generic.maxHealth")
                .getBaseValue();
            if (
                maxHealthBase == 13000d ||
                (
                    maxHealthBase == 2000d &&
                    enderman
                        .getHeldBlockState()
                        .getBlock()
                        .equals(Blocks.end_portal_frame)
                )
            ) {
                return LocationHandler
                    .getCurrentLocation()
                    .equals(Locations.DRAGONSNEST);
            }
        }
        return false;
    }
}
