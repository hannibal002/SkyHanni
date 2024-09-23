package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.features.mining.FlowstateHelper;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.List;

public class FlowstateHelperConfig {
    @Expose
    @ConfigOption(name = "Flowstate Helper", desc = "Shows stats for the Flowstate enchantment.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Flowstate Appearance", desc = "Drag text to change the appearance.")
    @ConfigEditorDraggableList()
    public List<FlowstateHelper.GUIElements> appearance = FlowstateHelper.GUIElements.defaultOption;

    @Expose
    @ConfigLink(owner = MiningConfig.class, field = "flowstateHelper")
    public Position position = new Position(-110 , 9);
}
