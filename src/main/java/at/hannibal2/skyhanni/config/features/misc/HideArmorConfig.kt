package at.hannibal2.skyhanni.config.features.misc

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HideArmorConfig {
    @Expose
    @ConfigOption(name = "Mode", desc = "Hide the armor of players.")
    @ConfigEditorDropdown
    var mode: ModeEntry = ModeEntry.OFF

    enum class ModeEntry(private val displayName: String) {
        ALL("All"),
        OWN("Own Armor"),
        OTHERS("Other's Armor"),
        OFF("Off"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Only Helmet", desc = "Only hide the helmet.")
    @ConfigEditorBoolean
    var onlyHelmet: Boolean = false
}
