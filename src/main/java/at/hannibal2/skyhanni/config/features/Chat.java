package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigAccordionId;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorAccordion;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class Chat {

    @Expose
    @ConfigOption(name = "Chat Filter Types", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean filterTypes = false;

    @Expose
    @ConfigOption(name = "HyPixel Hub", desc = "Block messages outside SkyBlock in the HyPixel lobby: player joins, loot boxes, prototype lobby messages, radiating generosity and HyPixel tournaments.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean hypixelHub = false;

    @Expose
    @ConfigOption(name = "Empty", desc = "Hide all the empty messages from the chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean empty = false;

    @Expose
    @ConfigOption(name = "Warping", desc = "Block 'sending request to join ..' and 'warping ..' messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean warping = false;

    @Expose
    @ConfigOption(name = "Welcome", desc = "Hide the 'welcome to skyblock' message.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean welcome = false;

    //TODO remove
    @Expose
    @ConfigOption(name = "Others", desc = "Hide other annoying messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean others = false;

    @Expose
    @ConfigOption(name = "Player Messages", desc = "Add a fancy new chat format for player messages.")
    @ConfigEditorBoolean
    public boolean playerMessages = false;

    @Expose
    @ConfigOption(name = "Dungeon Filter", desc = "Hide annoying messages inside dungeon.")
    @ConfigEditorBoolean
    public boolean dungeonMessages = false;

    @Expose
    @ConfigOption(name = "Dungeon Boss Messages", desc = "Hide messages from watcher and bosses inside dungeon.")
    @ConfigEditorBoolean
    public boolean dungeonBossMessages = false;
}
