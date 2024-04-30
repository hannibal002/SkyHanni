package at.hannibal2.skyhanni.config.features.rift;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class PunchcardConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Highlights unpunched players in the Rift.")
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> enabled = Property.of(false);

    @Expose
    @ConfigOption(name = "Color", desc = "Color used for highlighting.")
    @ConfigEditorColour
    public Property<String> color = Property.of("0:163:122:11:143");

    @Expose
    @ConfigOption(name = "enable gui", desc = "")
    @ConfigEditorBoolean
    public boolean gui = true;

    @Expose
    @ConfigOption(name = "comapct gui", desc = "")
    @ConfigEditorBoolean
    public Property<Boolean> compact = Property.of(false);

    @Expose
    @ConfigOption(name = "reverse", desc = "")
    @ConfigEditorBoolean
    public Property<Boolean> reverse = Property.of(false);

    @Expose
    @ConfigLink(owner = PunchcardConfig.class, field = "gui")
    public Position position = new Position(1, 2);
}
