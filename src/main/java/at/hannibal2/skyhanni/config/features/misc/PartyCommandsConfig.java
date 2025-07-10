package at.hannibal2.skyhanni.config.features.misc;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import org.jetbrains.annotations.NotNull;

public class PartyCommandsConfig {

    // TODO remove "default" from name
    @Expose
    @ConfigEditorDropdown
    @ConfigOption(name = "Trust Level", desc = "Choose who can run party chat commands.")
    public @NotNull TrustedUser defaultRequiredTrustLevel = TrustedUser.FRIENDS;

    // TODO should this be customizable per-command
    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Self-Trigger Commands", desc = "Allow party chat commands to trigger on your own messages.")
    public boolean selfTriggerCommands = false;

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Party Transfer", desc = "Automatically transfer the party to people who type §b!ptme§7.")
    public boolean transferCommand = false;

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Party Warp", desc = "Automatically warp the party if someone types §b!warp§7.")
    public boolean warpCommand = false;

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Party All Invite", desc = "Automatically turn on allinvite if someone types §b!allinv§7.")
    public boolean allInviteCommand = false;

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Ping", desc = "Sends current ping into Party Chat if someone types §b!ping§7.\n" +
        "§cNote: Will not work correctly with the Hypixel Ping API turned off in Dev.")
    public boolean pingCommand = false;

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "TPS", desc = "Sends current TPS into Party Chat if someone types §b!tps§7.")
    public boolean tpsCommand = false;

    public enum TrustedUser {
        BEST_FRIENDS("Best Friends"),
        FRIENDS("Friends"),
        ANYONE("Everyone"),
        NO_ONE("No One"),
        ;
        final String label;

        TrustedUser(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Show reminder", desc = "Show a reminder when an unauthorized player tries to run a command.")
    public boolean showIgnoredReminder = true;
}
