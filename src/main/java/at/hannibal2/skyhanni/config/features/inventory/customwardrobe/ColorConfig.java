package at.hannibal2.skyhanni.config.features.inventory.customwardrobe;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ColorConfig {

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

    @Expose
    @ConfigOption(name = "bordr thicc", desc = "")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 15,
        minStep = 1
    )
    public int outlineThickness = 5;

    @Expose
    @ConfigOption(name = "bordr blrrr", desc = "")
    @ConfigEditorSlider(
        minValue = 0f,
        maxValue = 1f,
        minStep = 0.1f
    )
    public float outlineBlur = 0.5f;


}
