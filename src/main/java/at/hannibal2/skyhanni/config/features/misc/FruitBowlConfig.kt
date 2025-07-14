package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class FruitBowlConfig {

    @ConfigOption(
        name = "Player Highlighter",
        desc = "Find players that want you to click on them to collect their profile names in your Fruit Bowl.",
    )
    @Expose
    @ConfigEditorBoolean
    @FeatureToggle
    var playerHighlighter: Boolean = true

    @Expose
    @ConfigOption(name = "Can Click", desc = "Color for players you have not yet clicked at.")
    @ConfigEditorColour
    val canColor: Property<String> = Property.of("0:1:85:255:85")

    @Expose
    @ConfigOption(name = "Already Clicked", desc = "Color for players you have already clicked at.")
    @ConfigEditorColour
    val canNotColor: Property<String> = Property.of("0:1:76:76:76")

    @Expose
    @ConfigOption(name = "Show Stats Display", desc = "Show a display with Fruit Bowl stats.")
    @ConfigEditorBoolean
    @FeatureToggle
    var display: Boolean = true

    @Expose
    @ConfigLink(owner = FruitBowlConfig::class, field = "display")
    val position: Position = Position(150, 200)
}
