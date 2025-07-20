package at.hannibal2.skyhanni.config.features.chat

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class UselessNotificationsFilterConfig {

    @Expose
    @ConfigOption(name = "No Bank Interest", desc = "Hides messages about having 0 bank interest.")
    @ConfigEditorBoolean
    var broke: Boolean = false

    @Expose
    @ConfigOption(name = "Power Orb New Location", desc = "Hides message about your power orb location being moved.")
    @ConfigEditorBoolean
    var powerOrb: Boolean = false

    @Expose
    @ConfigOption(name = "Mining Speed Boost", desc = "Hides mining speed boost notifications")
    @ConfigEditorBoolean
    var miningSpeed: Boolean = false
}
