package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.gui.core.config.Position;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorButton;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorColour;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class Ashfang {

    @Expose
    @ConfigOption(name = "Ashfang Freeze", desc = "Show the cooldown how long Ashfang blocks all your abilities.")
    @ConfigEditorBoolean
    public boolean freezeCooldown = false;

    @Expose
    @ConfigOption(name = "Ashfang Freeze Position", desc = "")
    @ConfigEditorButton(runnableId = "ashfangFreezeCooldown", buttonText = "Edit")
    public Position freezeCooldownPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Ashfang Reset", desc = "Shows the cooldown until ashfang pulls his underlings back")
    @ConfigEditorBoolean
    public boolean nextResetCooldown = false;

    @Expose
    @ConfigOption(name = "Ashfang Reset Position", desc = "")
    @ConfigEditorButton(runnableId = "ashfangResetCooldown", buttonText = "Edit")
    public Position nextResetCooldownPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Ashfang Gravity Orbs", desc = "Showing the Gravity Orbs more clearly")
    @ConfigEditorBoolean
    public boolean gravityOrbs = false;

    @Expose
    @ConfigOption(
            name = "Orbs Color",
            desc = "Color of the Ashfang Gravity Orbs"
    )
    @ConfigEditorColour
    public String gravityOrbsColor = "0:120:255:85:85";

    @Expose
    @ConfigOption(name = "Ashfang Blazing Souls", desc = "Showing the Blazing Souls more clearly")
    @ConfigEditorBoolean
    public boolean blazingSouls = false;

    @Expose
    @ConfigOption(
            name = "Souls Color",
            desc = "Color of the Ashfang Blazing Souls"
    )
    @ConfigEditorColour
    public String blazingSoulsColor = "0:245:85:255:85";

    @Expose
    @ConfigOption(name = "Ashfang Highlight Blazes", desc = "Highlight the different blazes in their respected color")
    @ConfigEditorBoolean
    public boolean highlightBlazes = false;

    @Expose
    @ConfigOption(name = "Ashfang Hide Particles", desc = "Hide all the particles around the ashfang boss")
    @ConfigEditorBoolean
    public boolean hideParticles = false;

    @Expose
    @ConfigOption(name = "Ashfang Hide Names", desc = "Hide the name of full health blazes around ashfang (only useful when highlight blazes is enabled)")
    @ConfigEditorBoolean
    public boolean hideNames = false;

    @Expose
    @ConfigOption(name = "Ashfang Hide Damage", desc = "Hide damage splashes around ashfang")
    @ConfigEditorBoolean
    public boolean hideDamageSplash = false;
}
