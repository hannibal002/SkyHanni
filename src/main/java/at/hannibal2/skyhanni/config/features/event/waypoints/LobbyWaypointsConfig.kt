package at.hannibal2.skyhanni.config.features.event.waypoints

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class LobbyWaypointsConfig {
    @Expose
    @ConfigOption(name = "Easter Egg WaypointSet", desc = "")
    @Accordion
    val easterEgg: EasterEggConfig = EasterEggConfig()

    @Expose
    @ConfigOption(name = "Halloween Basket WaypointSet", desc = "")
    @Accordion
    val halloweenBasket: HalloweenBasketConfig = HalloweenBasketConfig()

    @Expose
    @ConfigOption(name = "Christmas Present WaypointSet", desc = "")
    @Accordion
    val christmasPresent: ChristmasPresentConfig = ChristmasPresentConfig()
}
