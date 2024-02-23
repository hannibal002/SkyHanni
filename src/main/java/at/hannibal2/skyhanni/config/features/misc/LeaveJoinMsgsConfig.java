package at.hannibal2.skyhanni.config.features.misc;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LeaveJoinMsgsConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show a message in chat when a player leaves/joins your Private Island/Garden.\n§c§lMay be buggy in large hubs.")
    @ConfigEditorBoolean
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
        desc = "Show a message in chat when a player leaves/joins a SkyBlock island while guesting.")
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
        desc = "Only send a message if you know the person (they're in your friends, guild, or party).")
    @ConfigEditorBoolean
    public boolean onlyKnownPeople = false;

    @Expose
    @ConfigOption(
        name = "Always on Your Island",
        desc = "Bypass the Only Known Players toggle while on your Private Island/Garden.")
    @ConfigEditorBoolean
    public boolean alwaysOnYourIsland = false;

    @Expose
    @ConfigOption(
        name = "Always Known Islands",
        desc = "Bypass the Only Known Players toggle while visiting known players' Private Island/Garden.")
    @ConfigEditorBoolean
    public boolean alwaysOnKnownIslands = false;
}
