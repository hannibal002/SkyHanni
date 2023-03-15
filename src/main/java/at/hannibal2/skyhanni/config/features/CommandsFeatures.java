package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class CommandsFeatures {

    @Expose
    @ConfigOption(name = "Fandom Wiki", desc = "Using §ehypixel-skyblock.fandom.com §7instead of Hypixel wiki (§ewiki.hypixel.net§7).")
    @ConfigEditorBoolean
    public boolean useFandomWiki = false;
}
