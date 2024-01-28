package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class SensReducerConfig {
    @Expose
    @ConfigOption(
        name = "Auto Enable",
        desc = "Lowers mouse sensitivity while in the garden, and a farming tool is held.")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Reducing factor", desc = "Changes by how much the sensitivity is lowered by.")
    @ConfigEditorSlider(minValue = 1, maxValue = 100, minStep = 1)
    public Property<Float> reducingFactor = Property.of(15.0F);

    @Expose
    @ConfigOption(
        name = "Show GUI",
        desc = "Shows the GUI element while the feature is enabled.")
    @ConfigEditorBoolean
    public boolean showGUI = true;

    @Expose
    public Position position = new Position(400, 400, 0.8f);
}
