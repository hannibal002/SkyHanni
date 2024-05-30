package at.hannibal2.skyhanni.config.features.gui;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class XPBarConfig {

    @Expose
    @ConfigOption(name = "Editable", desc = "Adds the xp bar to the gui editor. Allows for moving and scaling of the xp bar.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean editable = false;

    @ConfigOption(name = "§cNotice", desc = "This option will be §c§lincompatible §r§7with mods that change the xp bar. Eg: §eApec§7.")
    @ConfigEditorInfoText
    public String notice = "";

    @Expose
    @ConfigLink(owner = XPBarConfig.class, field = "editable")
    public Position hotbar = new Position(20, 20);

    @Expose
    @ConfigOption(name = "Show Outside Skyblock", desc = "Enables it outside of SkyBlock.")
    @ConfigEditorBoolean
    public boolean showOutsideSkyblock = false;
}
