package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

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
        desc = "Only send a message if you know the person (they're in your friends, guild, party, or marked).")
    @ConfigEditorBoolean
    public boolean onlyKnownPeople = true;

    @Expose
    @ConfigOption(name = "Known Players Customization", desc = "")
    @Accordion
    public KnownPlayersDetails knownPlayersDetails = new KnownPlayersDetails();

    public static class KnownPlayersDetails {
        @Expose
        @ConfigOption(
            name = "Friends",
            desc = "Types of friends to include as known players.")
        @ConfigEditorDropdown
        public Property<IsFriendsKnown> isFriendsKnown = Property.of(IsFriendsKnown.ALL_FRIENDS);

        public enum IsFriendsKnown {
            ALL_FRIENDS("All"),
            BEST_FRIENDS("Only best"),
            NO_FRIENDS("None"),
            ;

            private final String str;

            IsFriendsKnown(String str) {
                this.str = str;
            }

            @Override
            public String toString() {
                return str;
            }
        }

        @Expose
        @ConfigOption(
            name = "Guild",
            desc = "Include guild members as known players.")
        @ConfigEditorBoolean
        public boolean isGuildKnown = true;

        @Expose
        @ConfigOption(
            name = "Party",
            desc = "Include party members as known players.")
        @ConfigEditorBoolean
        public boolean isPartyKnown = true;

        @Expose
        @ConfigOption(
            name = "Marked Players",
            desc = "Include marked players (§e/shmarkplayer§7) as known players.")
        @ConfigEditorBoolean
        public boolean isMarkedPlayersKnown = true;
    }

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
