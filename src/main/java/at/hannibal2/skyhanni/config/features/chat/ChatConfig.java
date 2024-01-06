package at.hannibal2.skyhanni.config.features.chat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

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
    @ConfigOption(name = "Dungeon Filter", desc = "Hide specific message types in Dungeons.")
    @ConfigEditorDraggableList()
    public List<DungeonMessageTypes> dungeonFilteredMessageTypes = Collections.emptyList();

    public enum DungeonMessageTypes {
        PREPARE("§bPreparation", "prepare"),
        START("§aClass Buffs §r/ §cMort Dialog", "start"),
        AMBIENCE("§bAmbience", "ambience"),
        PICKUP("§ePickup", "pickup"),
        REMINDER("§cReminder", "reminder"),
        BUFF("§dBlessings", "buff"),
        NOT_POSSIBLE("§cNot possible", "not_possible"),
        DAMAGE("§cDamage", "damage"),
        ABILITY("§dAbilities", "ability"),
        PUZZLE("§dPuzzle §r/ §cQuiz", "puzzle"),
        END("§7Essences§r/§aExperience", "end");

        private final String name;
        private final String key;

        DungeonMessageTypes(String name, String key) {
            this.name = name;
            this.key = key;
        }

        public Boolean hasKey(String key) {
            return this.key.equals(key);
        }

        @Override
        public String toString() {
            return name;
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
