//package com.thatgravyboat.skyblockhud.api.sbentities;
//
//import com.google.common.collect.ImmutableList;
//import com.google.common.collect.Maps;
//import java.util.List;
//import java.util.Map;
//import net.minecraft.entity.Entity;
//import net.minecraft.entity.monster.EntityEnderman;
//import net.minecraft.entity.monster.EntityZombie;
//
//public class EntityTypeRegistry {
//
//    private static final Map<Class<? extends Entity>, List<SkyBlockEntity>> entities = Maps.newHashMap();
//
//    static {
//        entities.put(EntityEnderman.class, ImmutableList.of(SkyBlockEntity.of("ZEALOT", EntityTypeHelper::isZealot)));
//        entities.put(EntityZombie.class, ImmutableList.of(SkyBlockEntity.of("CRYPT_GHOUL", EntityTypeHelper::isCrypt)));
//    }
//
//    public static String getEntityId(Entity entity) {
//        if (!entities.containsKey(entity.getClass())) return null;
//        for (SkyBlockEntity skyBlockEntity : entities.get(entity.getClass())) {
//            if (skyBlockEntity.isEntity(entity)) return skyBlockEntity.getName();
//        }
//        return null;
//    }
//}
