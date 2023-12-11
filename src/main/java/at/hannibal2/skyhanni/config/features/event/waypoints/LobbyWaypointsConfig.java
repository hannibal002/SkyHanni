package at.hannibal2.skyhanni.config.features.event.waypoints;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LobbyWaypointsConfig {

    @Expose
    @ConfigOption(name = "Halloween Basket Waypoints", desc = "")
    @Accordion
    public HalloweenBasketConfig halloweenBasket = new HalloweenBasketConfig();

    @Expose
    @ConfigOption(name = "Christmas Present Waypoints", desc = "")
    @Accordion
    public ChristmasPresentConfig christmasPresent = new ChristmasPresentConfig();
}
