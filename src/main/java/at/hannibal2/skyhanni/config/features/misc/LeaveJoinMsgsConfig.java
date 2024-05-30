package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class LeaveJoinMsgsConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show a message in chat when a player leaves/joins your Private Island/Garden.\n§c§lMay be buggy in large hubs.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(
        name = "On Public Islands",
        desc = "Show a message in chat when a player leaves/joins a Public SkyBlock island (instead of only Private Island/Garden).")
    @ConfigEditorBoolean
    public boolean onPublicIslands = false;

    @Expose
    @ConfigOption(
        name = "When Guesting",
        desc = "Show leave/join messages while guesting.")
    @ConfigEditorBoolean
    public boolean guestLeaveJoinMsgs = false;

    @Expose
    @ConfigOption(
        name = "Colored Messages",
        desc = "Color the §ajoined §7and §cleft §7messages.")
    @ConfigEditorBoolean
    public boolean leaveJoinColor = true;

    @Expose
    @ConfigOption(
        name = "Only Known Players",
        desc = "Only send a message if you know the person (customizable in the Advanced Player List Config).")
    @ConfigEditorBoolean
    public boolean onlyKnownPeople = true;

    @Expose
    @ConfigOption(
        name = "Always on Your Island",
        desc = "Bypass the Only Known Players toggle while on your Private Island/Garden.")
    @ConfigEditorBoolean
    public boolean alwaysOnYourIsland = true;

    @Expose
    @ConfigOption(
        name = "Always on Known Islands",
        desc = "Bypass the Only Known Players toggle while visiting known players' Private Island/Garden.\n§cMay be buggy with offline owners.")
    @ConfigEditorBoolean
    public boolean alwaysOnKnownIslands = false;
}
