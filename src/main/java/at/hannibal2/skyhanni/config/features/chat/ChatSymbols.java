package at.hannibal2.skyhanni.config.features.chat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.HasLegacyId;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class ChatSymbols {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Adds extra symbols to the chat such as those from ironman, " +
        "stranded, bingo or nether factions and places them next to your regular player emblems. " +
        "§cDoes not work with hide rank hider!")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Chat Symbol Location", desc = "Determines where the symbols should go in chat in relation to the " +
        "player's name. Hidden will hide all emblems from the chat. §eRequires above setting to be on to hide the symbols.")
    @ConfigEditorDropdown()
    public SymbolLocationEntry symbolLocation = SymbolLocationEntry.LEFT;

    public enum SymbolLocationEntry implements HasLegacyId {
        LEFT("Left", 0),
        RIGHT("Right", 1),
        HIDDEN("Hidden", 2);

        private final String str;
        private final int legacyId;

        SymbolLocationEntry(String str, int legacyId) {
            this.str = str;
            this.legacyId = legacyId;
        }

        // Constructor if new enum elements are added post-migration
        SymbolLocationEntry(String str) {
            this(str, -1);
        }

        @Override
        public int getLegacyId() {
            return legacyId;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
