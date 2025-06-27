package at.hannibal2.skyhanni.config.features.inventory.customwardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ColorConfig {

    companion object {
        private val config get() = SkyHanniMod.feature.inventory.customWardrobe
        @JvmStatic
        fun resetColor() {
            // Instead of manually resetting each color, just reset the entire config object
            config.color = ColorConfig()
        }
    }

    @ConfigOption(name = "Reset to Default", desc = "Reset all custom wardrobe color settings to the default.")
    @ConfigEditorButton(buttonText = "Reset")
    var resetColor: Runnable = Runnable(ColorConfig::resetColor)

    @Expose
    @ConfigOption(name = "Background", desc = "Color of the GUI background.")
    @ConfigEditorColour
    var backgroundColor: ChromaColour = ChromaColour.fromStaticRGB(0, 0, 0, 127)

    @Expose
    @ConfigOption(name = "Equipped", desc = "Color of the currently equipped wardrobe slot.")
    @ConfigEditorColour
    var equippedColor: ChromaColour = ChromaColour.fromStaticRGB(85, 255, 85, 127)

    @Expose
    @ConfigOption(name = "Favorite", desc = "Color of the wardrobe slots that have been added as favorites.")
    @ConfigEditorColour
    var favoriteColor: ChromaColour = ChromaColour.fromStaticRGB(255, 85, 85, 127)

    @Expose
    @ConfigOption(name = "Same Page", desc = "Color of wardrobe slots in the same page.")
    @ConfigEditorColour
    var samePageColor: ChromaColour = ChromaColour.fromStaticRGB(94, 108, 255, 127)

    @Expose
    @ConfigOption(name = "Other Page", desc = "Color of wardrobe slots in another page.")
    @ConfigEditorColour
    var otherPageColor: ChromaColour = ChromaColour.fromStaticRGB(0, 0, 0, 127)

    @Expose
    @ConfigOption(name = "Top Outline", desc = "Color of the top of the outline when hovered.")
    @ConfigEditorColour
    var topBorderColor: ChromaColour = ChromaColour.fromStaticRGB(255, 200, 0, 255)

    @Expose
    @ConfigOption(name = "Bottom Outline", desc = "Color of the bottom of the outline when hovered.")
    @ConfigEditorColour
    var bottomBorderColor: ChromaColour = ChromaColour.fromStaticRGB(255, 0, 0, 255)
}
