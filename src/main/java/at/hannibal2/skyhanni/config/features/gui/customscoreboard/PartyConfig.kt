package at.hannibal2.skyhanni.config.features.gui.customscoreboard

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class PartyConfig {
    @Expose
    @ConfigOption(
        name = "Max Party List",
        desc = "Max number of party members to show in the party list (you are not included)."
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 25f, minStep = 1f)
    val maxPartyList: Property<Int> = Property.of(4)

    @Expose
    @ConfigOption(
        name = "Show Party Everywhere",
        desc = "Show the party list everywhere.\n" +
            "If disabled, it will only show in Dungeon Hub, Crimson Isle & Kuudra."
    )
    @ConfigEditorBoolean
    var showPartyEverywhere: Boolean = false

    @Expose
    @ConfigOption(name = "Show Party Leader", desc = "Show the party leader in the party list.")
    @ConfigEditorBoolean
    var showPartyLeader: Boolean = true
}
