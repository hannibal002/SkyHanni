package at.hannibal2.skyhanni.config.features.commands;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class BetterWikiCommandConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Improve the functionality of the /wiki command.\nThis is required for all of the below features.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Use Fandom Wiki", desc = "Use Fandom Wiki (§ehypixel-skyblock.fandom.com§7) instead of the Hypixel wiki (§ewiki.hypixel.net§7) in most wiki-related chat messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean useFandom = false;

    @Expose
    @ConfigOption(name = "Auto Open", desc = "Directly opens the Wiki when running the command instead of having to click a message in chat.")
    @ConfigEditorBoolean
    public boolean autoOpenWiki = false;

    @Expose
    @ConfigOption(name = "Open from Menus", desc = "Directly opens the Wiki from menus instead of having to click a message in chat.")
    @ConfigEditorBoolean
    public boolean menuOpenWiki = false;

    @Expose
    @ConfigOption(name = "Fandom Wiki Key", desc = "Search for an item on Wiki with this keybind.\n§4For optimal experiences, do §lNOT§r §4bind this to a mouse button.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int wikiKeybind = Keyboard.KEY_NONE;
}
