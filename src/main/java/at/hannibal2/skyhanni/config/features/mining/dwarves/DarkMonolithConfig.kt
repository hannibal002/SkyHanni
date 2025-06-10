package at.hannibal2.skyhanni.config.features.mining.dwarves

import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class DarkMonolithConfig {

    @Expose
    @ConfigOption(name = "Tracker", desc = "Track mithril powder, coins, and Rock the Fish drops.")
    @ConfigEditorBoolean
    var tracker: Boolean = true

    @Expose
    @ConfigLink(owner = DarkMonolithConfig::class, field = "tracker")
    val trackerPosition: Position = Position(100, 100)

    @Expose
    @SearchTag("Monolith, Dark")
    @ConfigOption(
        name = "Highlight Dragon Egg",
        desc = "Highlight dragon eggs while in the Dwarven Mines. Useful for Dark Monolith hunting.\n" +
            "§cWill only work when you are in line of sight§7."
    )
    @ConfigEditorBoolean
    var highlight: Boolean = true

    @Expose
    @ConfigOption(
        name = "Title on LOS",
        desc = "Show a title on coming into line of sight with a dragon egg.\n" +
            "§eClear to disable§7."
    )
    @ConfigEditorText
    var title: String = "§5§lDark Monolith"

    @Expose
    @ConfigOption(name = "Highlight Color", desc = "What color to highlight the egg.")
    @ConfigEditorColour
    var highlightColor: ChromaColour = ChromaColour.fromStaticRGB(155, 29, 194, 75)

}
