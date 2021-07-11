package com.thatgravyboat.skyblockhud.api.events;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.Event;

public class SkyBlockEntityKilled extends Event {

    public String id;
    public Entity entity;

    public SkyBlockEntityKilled(String id, Entity entity) {
        this.id = id;
        this.entity = entity;
    }
}
