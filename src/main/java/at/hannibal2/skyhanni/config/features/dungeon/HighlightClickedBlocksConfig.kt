package at.hannibal2.skyhanni.config.features.dungeon

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.storage.NoReset
import at.hannibal2.skyhanni.config.storage.Resettable
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HighlightClickedBlocksConfig : Resettable() {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Highlight levers, chests, and wither essence when clicked in Dungeons.")
    @ConfigEditorBoolean
    @FeatureToggle
    @NoReset
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Chest Color", desc = "Color of clicked chests.")
    @ConfigEditorColour
    var chestColor: ChromaColour = ChromaColour.fromStaticRGB(85, 255, 85, 178)

    @Expose
    @ConfigOption(name = "Trapped Chest Color", desc = "Color of clicked trapped chests.")
    @ConfigEditorColour
    var trappedChestColor: ChromaColour = ChromaColour.fromStaticRGB(0, 170, 0, 178)

    @Expose
    @ConfigOption(name = "Locked Chest Color", desc = "Color of clicked locked chests.")
    @ConfigEditorColour
    var lockedChestColor: ChromaColour = ChromaColour.fromStaticRGB(255, 85, 85, 178)

    @Expose
    @ConfigOption(name = "Wither Essence Color", desc = "Color of clicked wither essence.")
    @ConfigEditorColour
    var witherEssenceColor: ChromaColour = ChromaColour.fromStaticRGB(255, 85, 255, 178)

    @Expose
    @ConfigOption(name = "Lever Color", desc = "Color of clicked levers.")
    @ConfigEditorColour
    var leverColor: ChromaColour = ChromaColour.fromStaticRGB(255, 255, 85, 178)

    @Expose
    @ConfigOption(name = "Show Text", desc = "Shows a text saying what you clicked with the highlight.")
    @ConfigEditorBoolean
    @NoReset
    var showText: Boolean = true

    @Expose
    @ConfigOption(name = "Random Color", desc = "If enabled makes the colors random.")
    @ConfigEditorBoolean
    @NoReset
    var randomColor: Boolean = false

    @ConfigOption(name = "Reset Colors", desc = "Resets the colors of the highlights to default ones.")
    @ConfigEditorButton(buttonText = "Reset")
    val reset: Runnable = Runnable(::reset)
}
