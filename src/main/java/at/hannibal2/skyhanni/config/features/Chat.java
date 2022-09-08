package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.gui.core.config.annotations.*;
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
    @ConfigOption(name = "Player Messages Format", desc = "Add a fancy new chat format for player messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean playerMessagesFormat = false;

    @Expose
    @ConfigOption(name = "Hide SkyBlock Level", desc = "Hiding the Skyblock Level from the chat messages")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean hideSkyblockLevel = false;

    @Expose
    @ConfigOption(
            name = "SkyBlock Level Design",
            desc = "Change the way the Skyblock Level gets displayed in the chat\n" +
                    "§cRequires SkyBlock Level and player messages format both enabled"
    )
    @ConfigEditorDropdown(
            values = {"§8[§6123§8] §bname §fmsg",
                    "§6§l123 §bname §fmsg",
                    "§bname §8[§6123§8]§f: msg"}
    )
    @ConfigAccordionId(id = 1)
    public int skyblockLevelDesign = 0;

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

    @Expose
    @ConfigOption(name = "Hide Far Deaths 2", desc = "Hide the death messages of other players, " +
            "except for players who are close to the player, inside dungeon or during a Kuudra fight.")
    @ConfigEditorBoolean
    public boolean hideFarDeathMessages2 = false;
}
