package at.hannibal2.skyhanni.config.features.skillprogress;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class SkillColorConfig {

    @Expose
    @ConfigOption(name = "Match Bar Color", desc = "Match the display color with the bar color.\n§cWill make the options below useless.")
    @ConfigEditorBoolean
    public Property<Boolean> matchBarColor = Property.of(false);

    @Expose
    @ConfigOption(name = "Enable Display Color", desc = "Change the text color in the display based on percentage completed.")
    @ConfigEditorBoolean
    public boolean enabledDisplayColor = false;

    @Expose
    @ConfigOption(name = "Text Display Color", desc = "Change the text display color based on percentage completed.\n" +
        "§eFollow the pattern start:end:color, each pattern separated by a ;")
    @ConfigEditorText
    public String displayPercentageColorString = "0:10:c;10:25:6;25:50:e;50:75:2;75:100:a";

    @Expose
    @ConfigOption(name = "Level Color", desc = "Change the level display color like Skyblock Level does.")
    @ConfigEditorBoolean
    public Property<Boolean> scalingColorLevel = Property.of(false);

}
