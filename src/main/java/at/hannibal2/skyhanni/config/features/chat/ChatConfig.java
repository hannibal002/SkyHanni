package at.hannibal2.skyhanni.config.features.chat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatConfig {

    @Expose
    @ConfigOption(name = "Peek Chat", desc = "Hold this key to keep the chat open.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_Z)
    public int peekChat = Keyboard.KEY_Z;

    @Expose
    @ConfigOption(name = "Chat Filter Types", desc = "")
    @Accordion
    public FilterTypesConfig filterType = new FilterTypesConfig();


    @Expose
    @ConfigOption(name = "Player Messages", desc = "")
    @Accordion
    public PlayerMessagesConfig playerMessage = new PlayerMessagesConfig();

    @Expose
    @ConfigOption(name = "Player Chat Symbols", desc = "")
    @Accordion
    public ChatSymbols chatSymbols = new ChatSymbols();

    @Expose
    @ConfigOption(name = "Dungeon Filter", desc = "Hide non-boss chat messages in Dungeons.")
    @ConfigEditorDropdown
    public DungeonFilterMode dungeonChatFilter = DungeonFilterMode.OFF;

    public enum DungeonFilterMode {
        ALL("All"), // ALL is empty because we are filtering everything
        ALL_NOT_BUFFS("All w/o buffs", Collections.singletonList("buff")),
        ALL_NOT_PUZZLES("All w/o puzzles", Collections.singletonList("puzzle")),
        // Not the best solution, but "never change a running code"
        ONLY_START_END("Only start/end messages", Arrays.asList(
            "unsorted", "pickup", "reminder", "buff", "not_possible", "damage", "ability", "puzzle"
        )),
        OFF("Off"); // OFF can be empty because the filter doesn't even run
        private final String str;
        private final List<String> whitelist;

        DungeonFilterMode(String str) {
            this(str, Collections.emptyList());
        }

        DungeonFilterMode(String str, List<String> whitelist) {
            this.str = str;
            this.whitelist = whitelist;
        }

        @Override
        public String toString() {
            return str;
        }

        public Boolean isFiltered(String key) {
            // If the list is empty, everything gets filtered
            // If the list does not contain the key, filter it
            return this.whitelist.isEmpty() || !this.whitelist.contains(key);
        }

        public Boolean isOff() {
            return this == DungeonFilterMode.OFF;
        }
    }

    @Expose
    @ConfigOption(name = "Dungeon Boss Messages", desc = "Hide messages from the Watcher and bosses in the Dungeon.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean dungeonBossMessages = false;

    @Expose
    @ConfigOption(name = "Hide Far Deaths", desc = "Hide other players' death messages, " +
        "except for players who are nearby or during Dungeons/a Kuudra fight.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideFarDeathMessages = false;
    //TODO jawbus + thunder

    @Expose
    @ConfigOption(name = "Compact Potion Messages", desc = "")
    @Accordion
    public CompactPotionConfig compactPotionMessages = new CompactPotionConfig();

    @Expose
    @ConfigOption(name = "Compact Bestiary Message", desc = "Shorten the Bestiary level up message, showing additional information when hovering.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean compactBestiaryMessage = true;

    @Expose
    @ConfigOption(name = "Arachne Hider", desc = "Hide chat messages about the Arachne Fight while outside of §eArachne's Sanctuary§7.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideArachneMessages = false;

    @Expose
    @ConfigOption(
        name = "Sacks Hider",
        desc = "Hide the chat's sack change message with this, " +
            "not in Hypixel settings, for mods to access sack data in new features."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideSacksChange = false;

    @Expose
    @ConfigOption(
        name = "Translator",
        desc = "Click on a message to translate it into English. " +
            "Use §e/shcopytranslation§7 to get the translation from English. " +
            "§cTranslation is not guaranteed to be 100% accurate."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean translator = false;
}
