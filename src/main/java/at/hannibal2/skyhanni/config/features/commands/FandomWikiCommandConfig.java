package at.hannibal2.skyhanni.config.features.commands;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class FandomWikiCommandConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Use Fandom Wiki (§ehypixel-skyblock.fandom.com§7) instead of the Hypixel wiki (§ewiki.hypixel.net§7) in most wiki-related chat messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Skip Chat", desc = "Directly opens the Fandom Wiki instead of sending the §e\"Click to search for this thing on the Fandom Wiki\"§7 message beforehand.")
    @ConfigEditorBoolean
    public boolean skipWikiChat = false;

    @Expose
    @ConfigOption(name = "Fandom Wiki Key", desc = "Search for an item on Fandom Wiki with this keybind.\n§4For optimal experiences, do §lNOT§r §4bind this to a mouse button.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int fandomWikiKeybind = Keyboard.KEY_NONE;
}
