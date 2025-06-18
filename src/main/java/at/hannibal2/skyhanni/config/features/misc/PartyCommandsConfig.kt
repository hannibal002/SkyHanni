package at.hannibal2.skyhanni.config.features.misc

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PartyCommandsConfig {
    @Expose
    @ConfigEditorDropdown
    @ConfigOption(name = "Party Command Trust Level", desc = "Choose who can run party chat commands.")
    var requiredTrustLevel: TrustedUser = TrustedUser.FRIENDS

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Party Transfer", desc = "Automatically transfer the party to people who type §b!ptme")
    var transferCommand: Boolean = false

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Party Warp", desc = "Automatically warp the party if someone types §b!warp")
    var warpCommand: Boolean = false

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Party All Invite", desc = "Automatically turn on allinvite if someone types §b!allinv")
    var allInviteCommand: Boolean = false

    enum class TrustedUser(private val displayName: String) {
        BEST_FRIENDS("Best Friends"),
        FRIENDS("Friends"),
        ANYONE("Everyone"),
        NO_ONE("No One"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Show reminder", desc = "Show a reminder when an unauthorized player tries to run a command.")
    var showIgnoredReminder: Boolean = true
}
