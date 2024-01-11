package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CrystalHollowsLobbyAgeWarningConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Toggles the overall warning.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Lobby Age Reminders", desc = "Toggles lobby age reminders before it passes a certain point.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean lobbyAgeReminders = false;

    @Expose
    @ConfigOption(name = "Player Count Reminders", desc = "Toggles player count reminders before it passes a certain point.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean playerCountReminders = false;

    @Expose
    @ConfigOption(name = "Minimum Players", desc = "")
    public int minPlayers = 4; //hidden config option that gets added to config.json in case hypixel changes these particular values and a hotfix isn't immediately possible -ery

    @Expose
    @ConfigOption(name = "Min Lobby Age Threshold (in MC Days)", desc = "")
    public int minLobbyAgeThreshold = 18; //hidden config option that gets added to config.json in case hypixel changes these particular values and a hotfix isn't immediately possible -ery

    @Expose
    @ConfigOption(name = "Max Lobby Age Threshold (in MC Days)", desc = "")
    public int maxLobbyAgeThreshold = 25; //hidden config option that gets added to config.json in case hypixel changes these particular values and a hotfix isn't immediately possible -ery
}
