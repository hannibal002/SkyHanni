package at.hannibal2.skyhanni.config.features.dungeon;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class DungeonChestConfig {


    @Expose
    @ConfigOption(name = "Kismet", desc = "Adds a visual highlight for used kismet feather to the Croesus inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean kismet = true;

    @Expose
    @ConfigOption(name = "Kismet Amount", desc = "Shows the amount of kismet feathers as stack size.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean kismetStackSize = true;
}
