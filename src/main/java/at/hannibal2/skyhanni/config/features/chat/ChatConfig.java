package at.hannibal2.skyhanni.config.features.chat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

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
    @ConfigOption(name = "Dungeon Filter", desc = "Hide annoying messages in Dungeons.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean dungeonMessages = true;

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
