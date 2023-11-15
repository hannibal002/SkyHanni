package at.hannibal2.skyhanni.config.features.event;

import at.hannibal2.skyhanni.config.features.event.bingo.BingoConfig;
import at.hannibal2.skyhanni.config.features.event.diana.DianaConfig;
import at.hannibal2.skyhanni.config.features.event.winter.WinterConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class EventConfig {

    @ConfigOption(name = "Monthly Bingo", desc = "")
    @Accordion
    @Expose
    public BingoConfig bingo = new BingoConfig();

    @ConfigOption(name = "Diana's Mythological Burrows", desc = "")
    @Accordion
    @Expose
    public DianaConfig diana = new DianaConfig();

    @ConfigOption(name = "Winter Season on Jerry's Island", desc = "")
    @Accordion
    @Expose
    public WinterConfig winter = new WinterConfig();

    @ConfigOption(name = "City Project", desc = "")
    @Accordion
    @Expose
    public CityProjectConfig cityProject = new CityProjectConfig();

    @ConfigOption(name = "Mayor Jerry's Jerrypocalypse", desc = "")
    @Accordion
    @Expose
    public MayorJerryConfig jerry = new MayorJerryConfig();

    @ConfigOption(name = "The Great Spook", desc = "")
    @Accordion
    @Expose
    public GreatSpookConfig spook = new GreatSpookConfig();

    // comment in if the event is needed again
//    @ConfigOption(name = "300þ Anniversary Celebration", desc = "Features for the 300þ year of SkyBlock")
    @Accordion
    @Expose
    public CenturyConfig century = new CenturyConfig();

    @Expose
    @ConfigOption(name = "Main Lobby Halloween Basket Waypoints", desc = "")
    @Accordion
    public HalloweenBasketConfig halloweenBasket = new HalloweenBasketConfig();

}
