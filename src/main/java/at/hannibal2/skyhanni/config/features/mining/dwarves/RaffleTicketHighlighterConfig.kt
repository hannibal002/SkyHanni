package at.hannibal2.skyhanni.config.features.mining.dwarves

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyLegacy
import at.hannibal2.skyhanni.config.OnlyModern
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import java.awt.Color

class RaffleTicketHighlighterConfig {

    @ConfigOption(
        name = "§cNotice",
        desc = "If you want to highlight raffle tickets turn on\n" +
            "/sh Glowing Dropped Items"
    )
    @ConfigEditorInfoText
    @OnlyLegacy
    var notice: String = ""

    @Expose
    @ConfigOption(name = "Enabled", desc = "Makes raffle tickets glow during the raffle event")
    @ConfigEditorBoolean
    @FeatureToggle
    @OnlyModern
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Color", desc = "Color that the raffle tickets will be highlighted in")
    @ConfigEditorColour
    @OnlyModern
    var ticketColor: ChromaColour = Color.GREEN.toChromaColor()
}
