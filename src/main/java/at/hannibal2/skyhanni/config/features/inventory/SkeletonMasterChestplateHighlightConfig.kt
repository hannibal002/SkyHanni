package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class SkeletonMasterChestplateHighlightConfig {

    @Expose
    @ConfigOption(
        name = "Good Chestplate Highlight",
        desc = "Highlights M7 (Tier 10), 50% Stat Boost Skeleton Master Chestplates in the 'Good Color' below."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    @SearchTag("Floor")
    var highlightGoodChestplate: Boolean = true

    @Expose
    @ConfigOption(name = "Good Color", desc = "What color to highlight 50% M7 Skeleton Master Chestplates in.")
    @ConfigEditorColour
    var goodColor: ChromaColour = LorenzColor.GREEN.toChromaColor()

    @Expose
    @ConfigOption(
        name = "Bad Chestplate Highlight",
        desc = "Highlights NON M7 (Tier 10), 50% Stat Boost Skeleton Master Chestplates in the 'Bad Color' below."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    @SearchTag("Floor")
    var highlightBadChestplate: Boolean = false

    @Expose
    @ConfigOption(name = "Bad Color", desc = "What color to highlight NON 50% M7 Skeleton Master Chestplates in.")
    @ConfigEditorColour
    var badColor: ChromaColour = LorenzColor.DARK_RED.toChromaColor()
}
