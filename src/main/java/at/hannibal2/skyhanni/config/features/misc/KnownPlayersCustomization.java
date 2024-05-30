package at.hannibal2.skyhanni.config.features.misc;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class KnownPlayersCustomization {
    @Expose
    @ConfigOption(
        name = "Marked Players",
        desc = "Include marked players (§e/shmarkplayer§7) as known players.")
    @ConfigEditorBoolean
    public boolean isMarkedPlayersKnown = true;

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
        name = "Party",
        desc = "Include party members as known players.")
    @ConfigEditorBoolean
    public boolean isPartyKnown = true;

    @Expose
    @ConfigOption(
        name = "Guild",
        desc = "Include guild members as known players.")
    @ConfigEditorBoolean
    public boolean isGuildKnown = true;
}
