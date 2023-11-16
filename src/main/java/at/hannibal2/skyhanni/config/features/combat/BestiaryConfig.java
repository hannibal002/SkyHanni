package at.hannibal2.skyhanni.config.features.combat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class BestiaryConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Show Bestiary Data overlay in the Bestiary menu.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Number format", desc = "Short: 1.1k\nLong: 1.100")
    @ConfigEditorDropdown(values = {"Short", "Long"})
    public int numberFormat = 0;

    @Expose
    @ConfigOption(name = "Display type", desc = "Choose what the display should show")
    @ConfigEditorDropdown(values = {
        "Global to max",
        "Global to next tier",
        "Lowest total kills",
        "Highest total kills",
        "Lowest kills needed to max",
        "Highest kills needed to max",
        "Lowest kills needed to next tier",
        "Highest kills needed to next tier"
    })
    public int displayType = 0;

    @Expose
    @ConfigOption(name = "Hide maxed", desc = "Hide maxed mobs.")
    @ConfigEditorBoolean
    public boolean hideMaxed = false;

    @Expose
    @ConfigOption(name = "Replace Romans", desc = "Replace Roman numerals (IX) with regular numbers (9)")
    @ConfigEditorBoolean
    public boolean replaceRoman = false;

    @Expose
    public Position position = new Position(100, 100, false, true);
}
