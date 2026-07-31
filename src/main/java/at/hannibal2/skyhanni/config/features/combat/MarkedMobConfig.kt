package at.hannibal2.skyhanni.config.features.combat

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.features.combat.mobs.MarkedMob
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class MarkedMobConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Let middle-clicking a bestiary entry mark the corresponding mob and highlight it in the world.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Highlight Color", desc = "The shared color used for marked mobs and highlighted beastiary slots.")
    @ConfigEditorColour
    val highlightColor: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(255, 255, 0, 255))

    @Expose
    val markedMobs = mutableSetOf<MarkedMob>()
}
