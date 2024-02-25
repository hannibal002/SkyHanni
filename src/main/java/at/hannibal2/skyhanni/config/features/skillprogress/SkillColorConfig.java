package at.hannibal2.skyhanni.config.features.skillprogress;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class SkillColorConfig {

    @Expose
    @ConfigOption(name = "Enable Display Color", desc = "Change the text color in the display based on percentage completed")
    @ConfigEditorBoolean
    public boolean enabledDisplayColor = false;

    @Expose
    @ConfigOption(name = "Text Display Color", desc = "Change the text display color based on percentage completed.\n" +
        "§eFollow the pattern start:end:color, each pattern separated by a ;")
    @ConfigEditorText
    public String displayPercentageColorString = "0:10:c;10:25:6;25:50:e;50:75:2;75:100:a";

}
