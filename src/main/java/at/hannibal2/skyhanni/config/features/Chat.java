package at.hannibal2.skyhanni.config.features;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.*;
import org.lwjgl.input.Keyboard;

public class Chat {

    @Expose
    @ConfigOption(name = "Peek Chat", desc = "Hold this key to keep the chat open.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_Z)
    public int peekChat = Keyboard.KEY_Z;

    @Expose
    @ConfigOption(name = "Chat Filter Types", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean filterTypes = false;

    @Expose
    @ConfigOption(name = "Hypixel Hub", desc = "Block messages outside SkyBlock in the Hypixel lobby: player joins, loot boxes, prototype lobby messages, radiating generosity and Hypixel tournaments.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean hypixelHub = true;

    @Expose
    @ConfigOption(name = "Empty", desc = "Hide all the empty messages from the chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean empty = true;

    @Expose
    @ConfigOption(name = "Warping", desc = "Block 'sending request to join...' and 'warping...' messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean warping = true;

    @Expose
    @ConfigOption(name = "Welcome", desc = "Hide the 'welcome to skyblock' message.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean welcome = true;

    @Expose
    @ConfigOption(name = "Guild Exp", desc = "Hide guild exp messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean guildExp = true;

    @Expose
    @ConfigOption(name = "Friend Join Left", desc = "Hide friend join/left messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean friendJoinLeft = false;

    @Expose
    @ConfigOption(name = "Winter Gifts", desc = "Hide useless winter gift messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean winterGift = false;

    @Expose
    @ConfigOption(name = "Powder Mining", desc = "Hide messages while opening chests in crystal hollows. " +
            "(Except powder numbers over 1k, Prehistoric Egg and Automaton Parts)")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean powderMining = true;

    @Expose
    @ConfigOption(name = "Kill Combo", desc = "Hide messages about the current kill combo from the Grandma Wolf Pet.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean killCombo = false;

    @Expose
    @ConfigOption(name = "Watchdog", desc = "Hide the message where hypixel is flexing how many players they have banned over the last week.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean watchDog = true;

    //TODO remove
    @Expose
    @ConfigOption(name = "Others", desc = "Hide other annoying messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean others = false;

    @Expose
    @ConfigOption(name = "Player Messages", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean playerMessages = false;

    @Expose
    @ConfigOption(name = "Player Rank Hider", desc = "Hide player ranks in all chat messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean playerRankHider = false;

    @Expose
    @ConfigOption(name = "Chat Filter", desc = "Scan messages sent by players for blacklisted words and grey out the message if any are found.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean chatFilter = false;

    @Expose
    @ConfigOption(name = "Dungeon Filter", desc = "Hide annoying messages in dungeons.")
    @ConfigEditorBoolean
    public boolean dungeonMessages = true;

    @Expose
    @ConfigOption(name = "Dungeon Boss Messages", desc = "Hide messages from watcher and bosses in the dungeon.")
    @ConfigEditorBoolean
    public boolean dungeonBossMessages = false;

    @Expose
    @ConfigOption(name = "Hide Far Deaths", desc = "Hide other players' death messages, " +
            "except for players who are nearby or during dungeons/a Kuudra fight.")
    @ConfigEditorBoolean
    public boolean hideFarDeathMessages = false;
    //TODO jawbus + x

    @Expose
    @ConfigOption(name = "Compact Potion Message", desc = "Shorten chat messages about player potion effects.")
    @ConfigEditorBoolean
    public boolean compactPotionMessage = true;
}
