package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.annotations.*;
import com.google.gson.annotations.Expose;

public class Chat {

    @Expose
    @ConfigOption(name = "Chat Filter Types", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean filterTypes = false;

    @Expose
    @ConfigOption(name = "Hypixel Hub", desc = "Block messages outside SkyBlock in the Hypixel lobby: player joins, loot boxes, prototype lobby messages, radiating generosity and Hypixel tournaments.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean hypixelHub = false;

    @Expose
    @ConfigOption(name = "Empty", desc = "Hide all the empty messages from the chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean empty = false;

    @Expose
    @ConfigOption(name = "Warping", desc = "Block 'sending request to join ..' and 'warping ..' messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean warping = false;

    @Expose
    @ConfigOption(name = "Welcome", desc = "Hide the 'welcome to skyblock' message.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean welcome = false;

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
    @ConfigOption(name = "All Channel Prefix", desc = "Show the prefix for the all channel chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean allChannelPrefix = false;

    @Expose
    @ConfigOption(name = "Player Rank Hider", desc = "Hide player ranks in the chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean playerRankHider = false;

    @Expose
    @ConfigOption(name = "Player Colon Hider", desc = "Hide the colon after the player name in the chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean playerColonHider = false;

    @Expose
    @ConfigOption(
            name = "SkyBlock Level Design",
            desc = "Change the design of the Skyblock Level in the chat.\n" +
                    "§cRequires 'SkyBlock Level in Chat' enabled in the SkyBlock Menu."
    )
    @ConfigEditorDropdown(
            values = {"§8[§6123§8] §bname§f: msg",
                    "§6§l123 §bname§f: §fmsg",
                    "§bname §8[§6123§8]§f: msg",
            "§cHide SkyBlock Level"}
    )
    @ConfigAccordionId(id = 1)
    public int skyblockLevelDesign = 0;

    @Expose
    @ConfigOption(
            name = "Elite Design",
            desc = "Change the design of the Elite position in the chat."
    )
    @ConfigEditorDropdown(
            values = {"§6[⌬499]",
                    "§6§l⌬499",
                    "§cHide Elite Position"}
    )
    @ConfigAccordionId(id = 1)
    public int eliteFormat = 0;

    @Expose
    @ConfigOption(
            name = "Channel Design",
            desc = "Change the design of the Channel Prefix in the chat."
    )
    @ConfigEditorDropdown(
            values = {"§2Guild >",
                    "§2G>",
                    "§8<§2G§8>",
                    "§8[§2G§8]",
                    "§8(§2G§8)"}
    )
    @ConfigAccordionId(id = 1)
    public int channelDesign = 0;

    @Expose
    @ConfigOption(name = "NEU Profile Viewer", desc = "Click on a player name to open the Profile Viewer from NotEnoughUpdates")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean neuProfileViewer = false;

    @Expose
    @ConfigOption(name = "Test All Chat", desc = "Test the All Chat message format locally (no message gets sent to hypixel)")
    @ConfigEditorButton(runnableId = "testAllChat")
    @ConfigAccordionId(id = 1)
    public boolean testAllChat = false;

    @Expose
    @ConfigOption(name = "Test Guild Chat", desc = "Test the Guild Chat message format locally (no message gets sent to hypixel)")
    @ConfigEditorButton(runnableId = "testGuildChat")
    @ConfigAccordionId(id = 1)
    public boolean testGuildChat = false;

    @Expose
    @ConfigOption(name = "Dungeon Filter", desc = "Hide annoying messages in the dungeon.")
    @ConfigEditorBoolean
    public boolean dungeonMessages = false;

    @Expose
    @ConfigOption(name = "Dungeon Boss Messages", desc = "Hide messages from watcher and bosses in the dungeon.")
    @ConfigEditorBoolean
    public boolean dungeonBossMessages = false;

    @Expose
    @ConfigOption(name = "Hide Far Deaths", desc = "Hide the death messages of other players, " +
            "except for players who are close to the player, inside dungeon or during a Kuudra fight.")
    @ConfigEditorBoolean
    public boolean hideFarDeathMessages = false;
}
