package com.thatgravyboat.skyblockhud.api.events;

import com.thatgravyboat.skyblockhud.location.Locations;
import net.minecraftforge.fml.common.eventhandler.Event;

public class LocationChangeEvent extends Event {

    public Locations oldLocation;
    public Locations newLocation;

    public LocationChangeEvent(Locations oldLocation, Locations newLocation) {
        this.oldLocation = oldLocation;
        this.newLocation = newLocation;
    }
}
