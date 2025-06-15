package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.HasLegacyId
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HideArmorConfig {
    @Expose
    @ConfigOption(name = "Mode", desc = "Hide the armor of players.")
    @ConfigEditorDropdown
    var mode: ModeEntry = ModeEntry.OFF

    enum class ModeEntry(
        private val displayName: String,
        private val legacyId: Int = -1,
    ) : HasLegacyId {
        ALL("All", 0),
        OWN("Own Armor", 1),
        OTHERS("Other's Armor", 2),
        OFF("Off", 3),
        ;

        override fun getLegacyId() = legacyId
        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Only Helmet", desc = "Only hide the helmet.")
    @ConfigEditorBoolean
    var onlyHelmet: Boolean = false
}
