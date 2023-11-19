package at.hannibal2.skyhanni.config.features.misc;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class HideArmorConfig {

    @Expose
    @ConfigOption(name = "Mode", desc = "Hide the armor of players.")
    @ConfigEditorDropdown(values = {"All", "Own Armor", "Other's Armor", "Off"})
    public int mode = 3;

    @Expose
    @ConfigOption(name = "Only Helmet", desc = "Only hide the helmet.")
    @ConfigEditorBoolean()
    public Boolean onlyHelmet = false;

}
