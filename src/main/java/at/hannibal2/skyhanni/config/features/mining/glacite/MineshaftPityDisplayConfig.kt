package at.hannibal2.skyhanni.config.features.mining.glacite

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
//#if TODO
import at.hannibal2.skyhanni.features.mining.MineshaftPityDisplay.MineshaftPityLine
//#endif
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

// todo 1.21 impl needed
class MineshaftPityDisplayConfig {
    @Expose
    @ConfigOption(name = "Enable Display", desc = "Enable the Mineshaft Pity Display.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    //#if TODO
    @Expose
    @ConfigOption(name = "Stats List", desc = "Drag text to change the appearance of the display.")
    @ConfigEditorDraggableList
    var mineshaftPityLines: MutableList<MineshaftPityLine> = mutableListOf(
        MineshaftPityLine.TITLE,
        MineshaftPityLine.COUNTER,
        MineshaftPityLine.CHANCE,
        MineshaftPityLine.NEEDED_TO_PITY,
        MineshaftPityLine.TIME_SINCE_MINESHAFT
    )
    //#endif

    @Expose
    @ConfigOption(name = "Modify Spawn Message", desc = "Modify the Mineshaft spawn message with more stats.")
    @ConfigEditorBoolean
    @FeatureToggle
    var modifyChatMessage: Boolean = true

    @Expose
    @ConfigLink(owner = MineshaftPityDisplayConfig::class, field = "enabled")
    var position: Position = Position(16, 192)
}
