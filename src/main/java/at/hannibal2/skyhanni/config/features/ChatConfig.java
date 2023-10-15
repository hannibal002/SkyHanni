package at.hannibal2.skyhanni.config.features;

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

    public static class FilterTypesConfig {
        @Expose
        @ConfigOption(name = "Hypixel Hub", desc = "Block messages outside SkyBlock in the Hypixel lobby: player joins, loot boxes, prototype lobby messages, radiating generosity and Hypixel tournaments.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hypixelHub = true;

        @Expose
        @ConfigOption(name = "Empty", desc = "Hide all the empty messages from the chat.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean empty = true;

        @Expose
        @ConfigOption(name = "Warping", desc = "Block 'Sending request to join...' and 'Warping...' messages.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean warping = true;

        @Expose
        @ConfigOption(name = "Welcome", desc = "Hide the 'Welcome to SkyBlock' message.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean welcome = true;

        @Expose
        @ConfigOption(name = "Guild Exp", desc = "Hide Guild EXP messages.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean guildExp = true;

        @Expose
        @ConfigOption(name = "Friend Join Left", desc = "Hide friend join/left messages.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean friendJoinLeft = false;

        @Expose
        @ConfigOption(name = "Winter Gifts", desc = "Hide useless Winter Gift messages.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean winterGift = false;

        @Expose
        @ConfigOption(name = "Powder Mining", desc = "Hide messages while opening chests in the Crystal Hollows. " +
                "(Except powder numbers over 1k, Prehistoric Egg and Automaton Parts)")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean powderMining = true;

        @Expose
        @ConfigOption(name = "Kill Combo", desc = "Hide messages about the current Kill Combo from the Grandma Wolf Pet.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean killCombo = false;

        @Expose
        @ConfigOption(name = "Watchdog", desc = "Hide the message where Hypixel is flexing how many players they have banned over the last week.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean watchDog = true;

        @Expose
        @ConfigOption(name = "Profile Join", desc = "Hide 'You are playing on profile' and 'Profile ID' chat messages.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean profileJoin = true;

        //TODO remove
        @Expose
        @ConfigOption(name = "Others", desc = "Hide other annoying messages.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean others = false;
    }

    @Expose
    @ConfigOption(name = "Player Messages", desc = "")
    @Accordion
    public PlayerMessagesConfig playerMessage = new PlayerMessagesConfig();

    public static class PlayerMessagesConfig {
        @Expose
        @ConfigOption(name = "Player Rank Hider", desc = "Hide player ranks in all chat messages.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean playerRankHider = false;

        @Expose
        @ConfigOption(name = "Chat Filter", desc = "Scan messages sent by players for blacklisted words and gray out the message if any are found.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean chatFilter = false;
    }

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
    @ConfigOption(name = "Compact Potion Message", desc = "Shorten chat messages about player potion effects.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean compactPotionMessage = true;

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

//    @Expose
//    @ConfigOption(
//            name = "Translator",
//            desc = "Click on a message to translate it into English. " +
//                    "Use §e/shcopytranslation§7 to get the translation from English. " +
//                    "§cTranslation is not guaranteed to be 100% accurate."
//    )
//    @ConfigEditorBoolean
//    @FeatureToggle
//    public boolean translator = false;
}
