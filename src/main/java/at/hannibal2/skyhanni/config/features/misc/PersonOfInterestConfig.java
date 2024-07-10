package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class PersonOfInterestConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable notification when a player from the list is in your lobby.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Players List", desc = "Players list you want to be notified for.\n§cCase sensitive, separated by comma.")
    @ConfigEditorText
    public Property<String> playersList = Property.of("hypixel");

    @Expose
    @ConfigOption(name = "Use Prefix", desc = "Should the [SkyHanni] prefix should be in the join/leave message ?")
    @ConfigEditorBoolean
    public boolean usePrefix = true;

    @Expose
    @ConfigOption(name = "Join Message", desc = "Configure the message when someone join.\n&& is replaced with the minecraft color code §.\n%s is replaced with the player name.")
    @ConfigEditorText
    public String joinMessage = "&&b%s &&ajoined your lobby.";

    @Expose
    @ConfigOption(name = "Left Message", desc = "Configure the message when someone leave.\n&& is replaced with the minecraft color code §.\n%s is replaced with the player name.")
    @ConfigEditorText
    public String leftMessage = "&&b%s &&cleft your lobby.";
}
