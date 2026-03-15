package at.hannibal2.skyhanni.config.features.slayer

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.IndividualItemTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class SlayerProfitTrackerConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Count all items you pick up while doing slayer, " +
            "keeping track of how much you pay for starting slayers and calculating the overall profit.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = SlayerProfitTrackerConfig::class, field = "enabled")
    val pos: Position = Position(20, 20)

    // TODO move out of slayer profit tracker and into the generic slayer config.
    @Expose
    @ConfigOption(
        name = "Voidgloom in Dragon's Nest",
        desc = "Show all Enderman Slayer Features while in the Dragon's Nest.",
    )
    @ConfigEditorBoolean
    val voidgloomInNest: Property<Boolean> = Property.of(false)

    // TODO move out of slayer profit tracker and into the generic slayer config.
    @Expose
    @ConfigOption(
        name = "Voidgloom Everywhere",
        desc = "Show all Enderman Slayer Features while outside of Void Sepulture and Zealot Bruiser Hideout.",
    )
    @ConfigEditorBoolean
    val voidgloomInNoArea: Property<Boolean> = Property.of(true)

    // TODO move out of slayer profit tracker and into the generic slayer config.
    @Expose
    @ConfigOption(
        name = "Revenant In Graveyard",
        desc = "Show all Revenant Slayer Features while inside the Graveyard.",
    )
    @ConfigEditorBoolean
    val revenantInGraveyard: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = ""
    )
    @Accordion
    val perTrackerConfig: IndividualItemTrackerConfig = IndividualItemTrackerConfig()
}
