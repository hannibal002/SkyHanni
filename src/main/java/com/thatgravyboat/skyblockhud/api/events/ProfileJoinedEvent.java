package com.thatgravyboat.skyblockhud.api.events;

import net.minecraftforge.fml.common.eventhandler.Event;

public class ProfileJoinedEvent extends Event {

    public String profile;

    public ProfileJoinedEvent(String profile){
        this.profile = profile;
    }
}
