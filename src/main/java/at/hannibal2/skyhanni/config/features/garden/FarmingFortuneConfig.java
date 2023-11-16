package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.commands.Commands;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorButton;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class FarmingFortuneConfig {
    @Expose
    @ConfigOption(
        name = "FF Display",
        desc = "Displays the true Farming Fortune for the current crop, including all crop-specific and hidden bonuses."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean display = true;

    @Expose
    @ConfigOption(
        name = "Show As Drop Multiplier",
        desc = "Adds 100 to the displayed Farming Fortune so that it represents a drop multiplier rather than" +
            " the chance for bonus drops. "
    )
    @ConfigEditorBoolean
    public boolean dropMultiplier = true;

    @ConfigOption(name = "Farming Fortune Guide", desc = "Opens a guide that breaks down your Farming Fortune.\n§eCommand: /ff")
    @ConfigEditorButton(buttonText = "Open")
    public Runnable open = Commands::openFortuneGuide;

    @Expose
    public Position pos = new Position(5, -180, false, true);
}
