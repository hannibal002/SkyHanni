package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.gui.core.config.Position;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorButton;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class Abilities {

    @Expose
    @ConfigOption(name = "Ability Cooldown", desc = "Show the cooldown of item abilities.")
    @ConfigEditorBoolean
    public boolean itemAbilityCooldown = false;

    @Expose
    @ConfigOption(name = "Ability Cooldown Background", desc = "Show the cooldown color of item abilities in the background.")
    @ConfigEditorBoolean
    public boolean itemAbilityCooldownBackground = false;

    @Expose
    @ConfigOption(name = "Ashfang Freeze", desc = "Show the cooldown how long Ashfang blocks all your abilities.")
    @ConfigEditorBoolean
    public boolean ashfangFreezeCooldown = false;

    @Expose
    @ConfigOption(name = "Ashfang Freeze Position", desc = "")
    @ConfigEditorButton(runnableId = "ashfangFreezeCooldown", buttonText = "Edit")
    public Position ashfangFreezeCooldownPos = new Position(10, 10, false, true);
}
