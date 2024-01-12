package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CrystalHollowsLobbyAgeWarningConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Toggles chat warnings when a Crystal Hollows lobby is about to close.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Lobby Age Reminders", desc = "Toggles lobby age reminders before it passes a certain point.")
    @ConfigEditorBoolean
    public boolean lobbyAgeReminders = false;

    @Expose
    @ConfigOption(name = "Player Count Reminders", desc = "Toggles player count reminders before it passes a certain point.")
    @ConfigEditorBoolean
    public boolean playerCountReminders = false;
}
