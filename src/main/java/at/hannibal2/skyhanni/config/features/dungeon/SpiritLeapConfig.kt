package at.hannibal2.skyhanni.config.features.dungeon

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SpiritLeapConfig {
    val defaultColor: String = "0:200:0:0:0"
    private val deadColor: String = "0:200:120:0:0"

    @Expose
    @ConfigOption(name = "Enable Spirit Leap Overlay", desc = "Enable Spirit Leap Overlay inside Dungeons.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Show Player Class Level",
        desc = "Display the player's Class level in the Spirit Leap overlay.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showDungeonClassLevel: Boolean = false

    @Expose
    @ConfigOption(
        name = "Dead Teammate Color",
        desc = "Set the highlight color for dead teammates in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var deadTeammateColor: String = "0:200:120:0:0"

    @Expose
    @ConfigOption(
        name = "Archer Class Color",
        desc = "Set the highlight color for the Archer class in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var archerClassColor: String = defaultColor

    @Expose
    @ConfigOption(
        name = "Mage Class Color",
        desc = "Set the highlight color for the Mage class in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var mageClassColor: String = defaultColor

    @Expose
    @ConfigOption(
        name = "Berserk Class Color",
        desc = "Set the highlight color for the Berserk class in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var berserkClassColor: String = defaultColor

    @Expose
    @ConfigOption(
        name = "Tank Class Color",
        desc = "Set the highlight color for the Tank class in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var tankClassColor: String = defaultColor

    @Expose
    @ConfigOption(
        name = "Healer Class Color",
        desc = "Set the highlight color for the Healer class in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var healerClassColor: String = defaultColor

    @ConfigOption(name = "Reset Colors", desc = "Restores the class highlighter colors to their default settings.")
    @ConfigEditorButton(buttonText = "Reset")
    var resetColors: Runnable = Runnable {
        deadTeammateColor = deadColor
        archerClassColor = defaultColor
        mageClassColor = defaultColor
        berserkClassColor = defaultColor
        tankClassColor = defaultColor
        healerClassColor = defaultColor
    }
}
