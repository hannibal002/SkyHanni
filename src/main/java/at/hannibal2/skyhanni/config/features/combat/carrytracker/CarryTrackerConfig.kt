package at.hannibal2.skyhanni.config.features.combat.carrytracker

import at.hannibal2.skyhanni.config.NoConfigLink
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CarryTrackerConfig {
    @ConfigOption(
        name = "Note",
        desc = "Type §e/shcarry §rto manage the carry tracker.",
    )
    @ConfigEditorInfoText
    val notice: String = ""

    @Expose
    @NoConfigLink
    val display: Position = Position(10, 10)

    @Expose
    @ConfigOption(
        name = "Suggest Carries From Trades",
        desc = "Detect coins traded and suggest adding carries based on configured prices.",
    )
    @ConfigEditorBoolean
    var suggestCarriesFromTrades: Boolean = true
}
