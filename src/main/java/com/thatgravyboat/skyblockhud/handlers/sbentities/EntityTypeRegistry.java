package com.thatgravyboat.skyblockhud.handlers.sbentities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityEnderman;

public class EntityTypeRegistry {

    private static final Map<Class<? extends Entity>, List<SkyBlockEntity>> entities = Maps.newHashMap();

    static {
        entities.put(
            EntityEnderman.class,
            ImmutableList.of(
                SkyBlockEntity.of("zealot", EntityTypeHelper::isZealot)
            )
        );
    }

    public static String getEntityId(Entity entity) {
        if (!entities.containsKey(entity.getClass())) return null;
        for (SkyBlockEntity skyBlockEntity : entities.get(
            entity.getClass()
        )) if (skyBlockEntity.isEntity(entity)) return skyBlockEntity.getName();
        return null;
    }
}
