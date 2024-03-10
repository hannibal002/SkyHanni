package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import at.hannibal2.skyhanni.utils.RenderUtils;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class TitleAndFooterConfig {
    @Expose
    @ConfigOption(name = "Title and Footer Alignment", desc = "Align the title and footer in the scoreboard.")
    @ConfigEditorDropdown
    public RenderUtils.HorizontalAlignment alignTitleAndFooter = RenderUtils.HorizontalAlignment.CENTER;

    @Expose
    @ConfigOption(name = "Custom Title", desc = "What should be displayed as the title of the scoreboard.\nUse & for colors.")
    @ConfigEditorText
    public Property<String> customTitle = Property.of("&6&lSKYBLOCK");

    @Expose
    @ConfigOption(name = "Hypixel's Title Animation", desc = "Will overwrite the custom title with Hypixel's title animation." +
        "\nWill also include \"COOP\" if you are in a coop.")
    @ConfigEditorBoolean
    public boolean useHypixelTitleAnimation = false;

    @Expose
    @ConfigOption(name = "Custom Footer", desc = "What should be displayed as the footer of the scoreboard.\nUse & for colors.")
    @ConfigEditorText
    public Property<String> customFooter = Property.of("&ewww.hypixel.net");
}
