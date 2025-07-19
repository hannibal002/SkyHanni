package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AuctionHousePriceComparisonConfig {
    @Expose
    @ConfigOption(
        name = "Show Price Comparison",
        desc = "Highlight auctions based on the difference between their estimated value and the value they are listed for.\n" +
            "§eThis may be very inaccurate at times and only provides an estimate."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Good Color", desc = "What color to highlight good value items with.")
    @ConfigEditorColour
    var good: ChromaColour = LorenzColor.GREEN.toChromaColor()

    @Expose
    @ConfigOption(name = "Very Good Color", desc = "What color to highlight very good value items with.")
    @ConfigEditorColour
    var veryGood: ChromaColour = ChromaColour.fromStaticRGB(0, 139, 0, 255)

    @Expose
    @ConfigOption(name = "Bad Color", desc = "What color to highlight bad items with.")
    @ConfigEditorColour
    var bad: ChromaColour = LorenzColor.YELLOW.toChromaColor()

    @Expose
    @ConfigOption(name = "Very Bad Color", desc = "What color to highlight very bad items with.")
    @ConfigEditorColour
    var veryBad: ChromaColour = ChromaColour.fromStaticRGB(225, 43, 30, 255)
}
