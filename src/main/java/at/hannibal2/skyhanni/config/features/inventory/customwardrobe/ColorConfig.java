package at.hannibal2.skyhanni.config.features.inventory.customwardrobe;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ColorConfig {

    @Expose
    @ConfigOption(name = "bg colr", desc = "")
    @ConfigEditorColour
    public String backgroundColor = "0:127:0:0:0";

    @Expose
    @ConfigOption(name = "equip colr", desc = "")
    @ConfigEditorColour
    public String equippedColor = "0:127:85:255:85";

    @Expose
    @ConfigOption(name = "fav colr", desc = "")
    @ConfigEditorColour
    public String favoriteColor = "0:127:255:85:85";

    @Expose
    @ConfigOption(name = "sam pag colr", desc = "")
    @ConfigEditorColour
    public String samePageColor = "0:127:94:108:255";

    @Expose
    @ConfigOption(name = "othr pge colr", desc = "")
    @ConfigEditorColour
    public String otherPageColor = "0:127:0:0:0";

    @Expose
    @ConfigOption(name = "top bordr colr", desc = "")
    @ConfigEditorColour
    public String topBorderColor = "0:255:255:200:0";

    @Expose
    @ConfigOption(name = "botm bordr colr", desc = "")
    @ConfigEditorColour
    public String bottomBorderColor = "0:255:255:0:0";

}
