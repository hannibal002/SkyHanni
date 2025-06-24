package at.hannibal2.skyhanni.config.features.mining.glacite

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import org.lwjgl.input.Keyboard

class TunnelMapsConfig {
    @Expose
    @ConfigOption(
        name = "Enable",
        desc = "Enable the tunnel maps, which give you a path to any location you want. Open your inventory to select a destination."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enable: Boolean = true

    @Expose
    @ConfigLink(owner = TunnelMapsConfig::class, field = "enable")
    var position: Position = Position(20, 20)

    @Expose
    @ConfigOption(
        name = "Auto Commission",
        desc = "Take the first collector commission as target when opening the commissions inventory, " +
            "also works when completing commissions."
    )
    @ConfigEditorBoolean
    var autoCommission: Boolean = false

    @Expose
    @ConfigOption(
        name = "Campfire Hotkey",
        desc = "Hotkey to warp to the campfire. If the travel scroll is not unlocked, show a path to the campfire instead."
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var campfireKey: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(
        name = "Travel Scroll",
        desc = "Let SkyHanni know that you have unlocked the §eTravel Scroll to Dwarven Base Camp§7."
    )
    @ConfigEditorBoolean
    var travelScroll: Boolean = false

    @Expose
    @ConfigOption(name = "Next Spot Hotkey", desc = "Hotkey to select the next spot.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var nextSpotHotkey: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(name = "Left Click Pigeon", desc = "Left click the Royal Pigeon to go to the next spot.")
    @ConfigEditorBoolean
    var leftClickPigeon: Boolean = true

    @Expose
    @ConfigOption(
        name = "Dynamic Path Color",
        desc = "Instead of the selected color use the color of the target as line color."
    )
    @ConfigEditorBoolean
    var dynamicPathColor: Boolean = true

    @Expose
    @ConfigOption(name = "Path Color", desc = "The color for the paths, if the dynamic color option is turned off.")
    @ConfigEditorColour
    var pathColor: String = "0:255:0:255:0"

    @Expose
    @ConfigOption(name = "Text Size", desc = "Size of the waypoint texts.")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 2.5f, minStep = 0.1f)
    var textSize: Float = 1f

    @Expose
    @ConfigOption(name = "Path width", desc = "Size of the path lines.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 15f, minStep = 1f)
    var pathWidth: Float = 4f

    @Expose
    @ConfigOption(name = "Distance at First", desc = "Show the distance at the first edge instead of the end.")
    @ConfigEditorBoolean
    var distanceFirst: Boolean = false

    @Expose
    @ConfigOption(name = "Compact Gemstone", desc = "Only show the icon for gemstones in the selection list.")
    @ConfigEditorBoolean
    var compactGemstone: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Exclude Fairy", desc = "Exclude the fairy soul spots from the selection list.")
    @ConfigEditorBoolean
    var excludeFairy: Property<Boolean> = Property.of(false)
}
