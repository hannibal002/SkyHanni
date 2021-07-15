package com.thatgravyboat.skyblockhud.api.events;

import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.Event;

public class SkyBlockEntityKilled extends Event {

    public String id;

    @Nullable
    public Entity entity;

    public SkyBlockEntityKilled(String id, @Nullable Entity entity) {
        this.id = id;
        this.entity = entity;
    }
}
