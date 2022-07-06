package com.thatgravyboat.skyblockhud.api.events;

import net.minecraftforge.fml.common.eventhandler.Event;

public class ProfileSwitchedEvent extends Event {

    public String profile;

    public ProfileSwitchedEvent(String profile) {
        this.profile = profile;
    }
}
