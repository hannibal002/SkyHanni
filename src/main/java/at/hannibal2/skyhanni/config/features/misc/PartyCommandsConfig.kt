package at.hannibal2.skyhanni.config.features.misc

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PartyCommandsConfig {
    @Expose
    @ConfigEditorDropdown
    @ConfigOption(name = "Trust Level", desc = "Choose who can run party chat commands.")
    var requiredTrustLevel: TrustedUser = TrustedUser.FRIENDS

    // TODO should this be customizable per-command
    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Self-Trigger Commands", desc = "Allow party chat commands to trigger on your own messages.")
    var selfTriggerCommands: Boolean = false

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Party Transfer", desc = "Automatically transfer the party to people who type §b!ptme§7.")
    var transferCommand: Boolean = false

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Party Warp", desc = "Automatically warp the party if someone types §b!warp§7.")
    var warpCommand: Boolean = false

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Party All Invite", desc = "Automatically turn on allinvite if someone types §b!allinv§7.")
    var allInviteCommand: Boolean = false

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(
        name = "Ping",
        desc = "Sends current ping into Party Chat if someone types §b!ping§7.\n" +
            "§cNote: Will not work correctly with the Hypixel Ping API turned off in Dev."
    )
    var pingCommand: Boolean = false

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "TPS", desc = "Sends current TPS into Party Chat if someone types §b!tps§7.")
    var tpsCommand: Boolean = false

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
