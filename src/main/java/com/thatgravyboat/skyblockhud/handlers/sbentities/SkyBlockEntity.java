package com.thatgravyboat.skyblockhud.handlers.sbentities;

import net.minecraft.entity.Entity;

import java.util.function.Predicate;

public class SkyBlockEntity {

    private final String name;
    private final Predicate<Entity> predicate;

    public static SkyBlockEntity of(String name, Predicate<Entity> predicate){
        return new SkyBlockEntity(name, predicate);
    }

    private SkyBlockEntity(String name, Predicate<Entity> predicate){
        this.name = name;
        this.predicate = predicate;
    }

    public String getName(){ return name; }

    public boolean isEntity(Entity entity) {
        return predicate.test(entity);
    }
}
