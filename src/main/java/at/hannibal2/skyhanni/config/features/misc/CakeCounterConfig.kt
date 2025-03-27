package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CakeCounterConfig {

    @ConfigOption(
        name = "Note",
        desc = "§cNote§7: The following features require a Cake Counter to be placed on your Private Island." +
            "\n§eFeatures may also not work as expected if your Cake Counter is not loaded in when on your island.",
    )
    @ConfigEditorInfoText
    var cakeCounterNote: Boolean = false

    @Expose
    @ConfigOption(
        name = "Soul Found Alert",
        desc = "Send a chat message if somebody finds a Cake Soul while you are on your Private Island.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var soulFoundAlert: Boolean = true

    @Expose
    @ConfigOption(
        name = "Offline Cake Counter",
        desc = "Send a chat message with Cake Counter stat changes while you were away from your Private Island.",
    )
    @ConfigEditorDropdown
    var offlineStatsMode: OfflineStatsMode = OfflineStatsMode.DISABLED

    enum class OfflineStatsMode(private val displayName: String) {
        DISABLED("Disabled"),
        CAKES_ONLY("Cakes Only"),
        SOULS_ONLY("Souls Only"),
        BOTH("Both");

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Tracking Mode",
        desc = "Choose how \"Offline Cake Counter\" tracks: Since last leaving or since last joining your Private Island.",
    )
    @ConfigEditorDropdown
    var offlineTrackingMode: OfflineTrackingMode = OfflineTrackingMode.SINCE_LAST_LEFT

    enum class OfflineTrackingMode(private val displayName: String) {
        SINCE_LAST_LEFT("Since Last Left"),
        SINCE_LAST_JOINED("Since Last Joined");

        override fun toString() = displayName
    }
}
