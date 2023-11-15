package at.hannibal2.skyhanni.config.features.rift;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.features.rift.area.RiftAreasConfig;
import at.hannibal2.skyhanni.config.features.rift.motes.MotesConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class RiftConfig {

    @ConfigOption(name = "Rift Timer", desc = "")
    @Accordion
    @Expose
    public RiftTimerConfig timer = new RiftTimerConfig();

    @ConfigOption(name = "Crux Talisman Progress", desc = "")
    @Accordion
    @Expose
    public CruxTalismanDisplayConfig cruxTalisman = new CruxTalismanDisplayConfig();

    @ConfigOption(name = "Enigma Soul Waypoints", desc = "")
    @Accordion
    @Expose
    public EnigmaSoulConfig enigmaSoulWaypoints = new EnigmaSoulConfig();

    @ConfigOption(name = "Rift Areas", desc = "")
    @Accordion
    @Expose
    public RiftAreasConfig area = new RiftAreasConfig();

    @Expose
    @ConfigOption(name = "Motes Sell Price", desc = "")
    @Accordion
    public MotesConfig motes = new MotesConfig();

    @Expose
    @ConfigOption(name = "Motes Orbs", desc = "")
    @Accordion
    public MotesOrbsConfig motesOrbs = new MotesOrbsConfig();

    @Expose
    @ConfigOption(name = "Highlight Guide", desc = "Highlight things to do in the Rift Guide.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightGuide = true;

    @Expose
    @ConfigOption(name = "Horsezooka Hider", desc = "Hide horses while holding the Horsezooka in the hand.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean horsezookaHider = false;
}
