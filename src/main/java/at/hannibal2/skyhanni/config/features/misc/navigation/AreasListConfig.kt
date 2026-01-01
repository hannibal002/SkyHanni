package at.hannibal2.skyhanni.config.features.misc.navigation

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class AreasListConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Shows all island areas as list while in your inventory. Click to navigate."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Show Always", desc = "Show the list even outside of inventories.")
    @ConfigEditorBoolean
    var showAlways: Boolean = false

    @Expose
    @ConfigOption(name = "Current Area", desc = "Show the current area at the top of the list.")
    @ConfigEditorBoolean
    val includeCurrentArea: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Path Color", desc = "Color of the navigation path.")
    @ConfigEditorColour
    val color: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(85, 255, 85, 245))

    @Expose
    @ConfigLink(owner = AreasListConfig::class, field = "enabled")
    val position: Position = Position(-350, 100)
}
