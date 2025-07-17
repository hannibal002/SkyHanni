package at.hannibal2.skyhanni.config.features.dungeon.spiritleap

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SpiritLeapColorConfig {

    companion object {
        @Transient const val DEFAULT_COLOR: String = "0:200:0:0:0"
        private const val DEAD_COLOR: String = "0:200:120:0:0"
    }

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
    var archerClassColor: String = DEFAULT_COLOR

    @Expose
    @ConfigOption(
        name = "Mage Class Color",
        desc = "Set the highlight color for the Mage class in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var mageClassColor: String = DEFAULT_COLOR

    @Expose
    @ConfigOption(
        name = "Berserk Class Color",
        desc = "Set the highlight color for the Berserk class in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var berserkClassColor: String = DEFAULT_COLOR

    @Expose
    @ConfigOption(
        name = "Tank Class Color",
        desc = "Set the highlight color for the Tank class in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var tankClassColor: String = DEFAULT_COLOR

    @Expose
    @ConfigOption(
        name = "Healer Class Color",
        desc = "Set the highlight color for the Healer class in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var healerClassColor: String = DEFAULT_COLOR

    @ConfigOption(name = "Reset Colors", desc = "Restores the class highlighter colors to their default settings.")
    @ConfigEditorButton(buttonText = "Reset")
    val resetColors: Runnable = Runnable {
        deadTeammateColor = DEAD_COLOR
        archerClassColor = DEFAULT_COLOR
        mageClassColor = DEFAULT_COLOR
        berserkClassColor = DEFAULT_COLOR
        tankClassColor = DEFAULT_COLOR
        healerClassColor = DEFAULT_COLOR
    }
}
