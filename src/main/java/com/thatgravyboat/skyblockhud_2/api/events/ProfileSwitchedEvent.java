package com.thatgravyboat.skyblockhud_2.api.events;

import net.minecraftforge.fml.common.eventhandler.Event;

public class ProfileSwitchedEvent extends Event {

    public String profile;

    public ProfileSwitchedEvent(String profile) {
        this.profile = profile;
    }
}
