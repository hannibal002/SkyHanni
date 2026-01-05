package at.hannibal2.skyhanni.config.features.rift.area.mountaintop

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MountaintopConfig {

    @Expose
    @ConfigOption(name = "Sun Gecko", desc = "")
    @Accordion
    val sunGecko: SunGeckoConfig = SunGeckoConfig()

    @Expose
    @ConfigOption(name = "Timite", desc = "")
    @Accordion
    val timite: TimiteConfig = TimiteConfig()

    @Expose
    @ConfigOption(
        name = "Flower Pot Enigma Soul Helper",
        desc = "Show the dropdown location to the hard to reach flower pot during the parkour for the Enigma Soul in Rose's End.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enigmaRoseFlowerpot: Boolean = true

    @Expose
    @ConfigOption(name = "Ubik's Cube Reminder", desc = "Reminder when the 2 hour cooldown is over for Ubik's Cube in the Rift.")
    @ConfigEditorBoolean
    @FeatureToggle
    var ubikReminder: Boolean = false

    @Expose
    @ConfigOption(name = "Ubik's Cube GUI", desc = "GUI that shows how long until Ubik's Cube is ready.")
    @ConfigEditorBoolean
    @FeatureToggle
    var ubikGui: Boolean = false

    @Expose
    @ConfigOption(name = "Only Show When Ready", desc = "Only show the Ubik's Cube GUI when it is ready.")
    @ConfigEditorBoolean
    var ubikOnlyWhenReady: Boolean = false

    @Expose
    @ConfigLink(owner = MountaintopConfig::class, field = "ubikGui")
    val timerPosition: Position = Position(100, 10)
}
