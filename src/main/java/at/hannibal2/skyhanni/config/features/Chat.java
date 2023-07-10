package at.hannibal2.skyhanni.config.features;

import io.github.moulberry.moulconfig.annotations.*;
import org.lwjgl.input.Keyboard;

public class Chat {

    @ConfigOption(name = "Peek Chat", desc = "Hold this key to keep the chat open.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_Z)
    public int peekChat = Keyboard.KEY_Z;

    @ConfigOption(name = "Chat Filter Types", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean filterTypes = false;

    @ConfigOption(name = "Hypixel Hub", desc = "Block messages outside SkyBlock in the Hypixel lobby: player joins, loot boxes, prototype lobby messages, radiating generosity and Hypixel tournaments.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean hypixelHub = true;

    @ConfigOption(name = "Empty", desc = "Hide all the empty messages from the chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean empty = true;

    @ConfigOption(name = "Warping", desc = "Block 'sending request to join...' and 'warping...' messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean warping = true;

    @ConfigOption(name = "Welcome", desc = "Hide the 'welcome to SkyBlock' message.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean welcome = true;

    @ConfigOption(name = "Guild Exp", desc = "Hide guild exp messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean guildExp = true;

    @ConfigOption(name = "Friend Join Left", desc = "Hide friend join/left messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean friendJoinLeft = false;

    @ConfigOption(name = "Winter Gifts", desc = "Hide useless winter gift messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean winterGift = false;

    @ConfigOption(name = "Powder Mining", desc = "Hide messages while opening chests in crystal hollows. " +
            "(Except powder numbers over 1k, Prehistoric Egg and Automaton Parts)")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean powderMining = true;

    @ConfigOption(name = "Kill Combo", desc = "Hide messages about the current kill combo from the Grandma Wolf Pet.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean killCombo = false;

    @ConfigOption(name = "Watchdog", desc = "Hide the message where Hypixel is flexing how many players they have banned over the last week.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean watchDog = true;

    @ConfigOption(name = "Profile Join", desc = "Hide 'You are playing on profile' and 'Profile ID' chat messages")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean profileJoin = true;

    //TODO remove
    @ConfigOption(name = "Others", desc = "Hide other annoying messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean others = false;

    @ConfigOption(name = "Player Messages", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean playerMessages = false;

    @ConfigOption(name = "Player Rank Hider", desc = "Hide player ranks in all chat messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean playerRankHider = false;

    @ConfigOption(name = "Chat Filter", desc = "Scan messages sent by players for blacklisted words and grey out the message if any are found.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean chatFilter = false;

    @ConfigOption(name = "Dungeon Filter", desc = "Hide annoying messages in dungeons.")
    @ConfigEditorBoolean
    public boolean dungeonMessages = true;

    @ConfigOption(name = "Dungeon Boss Messages", desc = "Hide messages from the watcher and bosses in the dungeon.")
    @ConfigEditorBoolean
    public boolean dungeonBossMessages = false;

    @ConfigOption(name = "Hide Far Deaths", desc = "Hide other players' death messages, " +
            "except for players who are nearby or during dungeons/a Kuudra fight.")
    @ConfigEditorBoolean
    public boolean hideFarDeathMessages = false;
    //TODO jawbus + thunder

    @ConfigOption(name = "Compact Potion Message", desc = "Shorten chat messages about player potion effects.")
    @ConfigEditorBoolean
    public boolean compactPotionMessage = true;

    @ConfigOption(name = "Arachne Hider", desc = "Hide chat messages about the Arachne Fight while outside of §eArachne's Sanctuary§7.")
    @ConfigEditorBoolean
    public boolean hideArachneMessages = false;
}
