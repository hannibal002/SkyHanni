package com.thatgravyboat.skyblockhud_2.api.events;

import net.minecraftforge.fml.common.eventhandler.Event;

public class ProfileJoinedEvent extends Event {

    public String profile;

    public ProfileJoinedEvent(String profile) {
        this.profile = profile;
    }
}
