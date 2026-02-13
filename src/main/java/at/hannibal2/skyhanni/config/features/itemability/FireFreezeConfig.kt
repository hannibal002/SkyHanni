package at.hannibal2.skyhanni.config.features.itemability

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FireFreezeConfig {

    @Expose
    @ConfigOption(name = "Mob Timer", desc = "Enables showing a timer above frozen mobs until they are unfrozen.")
    @ConfigEditorBoolean
    @FeatureToggle
    var mobTimer: Boolean = false

    @Expose
    @ConfigOption(name = "Freeze Timer", desc = "Enables showing a timer above active Fire Freezes until they freeze mobs.")
    @ConfigEditorBoolean
    @FeatureToggle
    var freezeTimer: Boolean = false

    @Expose
    @ConfigOption(
        name = "Box Frozen Mobs",
        desc = "Draws a box around frozen mobs" +
            " Color changes towards red the closer to re-freeze timing (5s post first freeze) it is.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var mobHighlight: Boolean = false

    @Expose
    @ConfigOption(
        name = "Custom Circle",
        desc = "Replaces the particle-based circle by Hypixel with a custom Circle similar to fire freeze.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var customCircle: Boolean = false

    @Expose
    @ConfigOption(name = "Freeze Circle Color", desc = "Changes the color of the Custom Circle.")
    @ConfigEditorColour
    var displayColor: ChromaColour = ChromaColour.fromStaticRGB(0, 0, 0, 245)

}
