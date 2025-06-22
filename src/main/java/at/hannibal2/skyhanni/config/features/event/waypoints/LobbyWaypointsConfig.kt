package at.hannibal2.skyhanni.config.features.event.waypoints

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class LobbyWaypointsConfig {
    @Expose
    @ConfigOption(name = "Easter Egg Waypoints", desc = "")
    @Accordion
    var easterEgg: EasterEggConfig = EasterEggConfig()

    @Expose
    @ConfigOption(name = "Halloween Basket Waypoints", desc = "")
    @Accordion
    var halloweenBasket: HalloweenBasketConfig = HalloweenBasketConfig()

    @Expose
    @ConfigOption(name = "Christmas Present Waypoints", desc = "")
    @Accordion
    var christmasPresent: ChristmasPresentConfig = ChristmasPresentConfig()
}
