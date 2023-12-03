package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class SocialConfig {

    @Expose
    @ConfigOption(name = "Show Player Join Message", desc = "Display a message in chat when a player joins your lobby")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean playerJoinNotifications = false;

    @Expose
    @ConfigOption(name = "Show Player Leave Message", desc = "Display a message in chat when a player leaves your lobby")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean playerLeaveNotifications = false;

    @Expose
    @ConfigOption(name = "Max Lobby Size", desc = "Only show player join/leave messages if the lobby is under X players:")
    @ConfigEditorSlider(minValue = 2f, maxValue = 100f, minStep = 1f)
    public float playerNotificationLobbySize = 12;

    @Expose
    @ConfigOption(name = "Show Lobby Size", desc = "Display the current number of players in your lobby")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showLobbySize = false;

    @Expose
    public Position showLobbySizePos = new Position(394, 142, false, true);

}
