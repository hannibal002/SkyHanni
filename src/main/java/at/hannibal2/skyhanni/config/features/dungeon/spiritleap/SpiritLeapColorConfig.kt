package at.hannibal2.skyhanni.config.features.dungeon.spiritleap

import at.hannibal2.skyhanni.config.storage.Resettable
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SpiritLeapColorConfig : Resettable() {

    companion object {
        @Transient val defaultColor = ChromaColour.fromStaticRGB(0, 0, 0, 200)
    }

    @Expose
    @ConfigOption(
        name = "Dead Teammate Color",
        desc = "Set the highlight color for dead teammates in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var deadTeammateColor: ChromaColour = ChromaColour.fromStaticRGB(120, 0, 0, 200)

    @Expose
    @ConfigOption(
        name = "Archer Class Color",
        desc = "Set the highlight color for the Archer class in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var archerClassColor: ChromaColour = defaultColor

    @Expose
    @ConfigOption(
        name = "Mage Class Color",
        desc = "Set the highlight color for the Mage class in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var mageClassColor: ChromaColour = defaultColor

    @Expose
    @ConfigOption(
        name = "Berserk Class Color",
        desc = "Set the highlight color for the Berserk class in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var berserkClassColor: ChromaColour = defaultColor

    @Expose
    @ConfigOption(
        name = "Tank Class Color",
        desc = "Set the highlight color for the Tank class in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var tankClassColor: ChromaColour = defaultColor

    @Expose
    @ConfigOption(
        name = "Healer Class Color",
        desc = "Set the highlight color for the Healer class in the Spirit Leap overlay.",
    )
    @ConfigEditorColour
    var healerClassColor: ChromaColour = defaultColor

    @ConfigOption(name = "Reset Colors", desc = "Restores the class highlighter colors to their default settings.")
    @ConfigEditorButton(buttonText = "Reset")
    val resetColors: Runnable = Runnable(::reset)
}
