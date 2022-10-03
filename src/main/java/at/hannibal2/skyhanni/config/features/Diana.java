package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.annotations.ConfigAccordionId;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorAccordion;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class Diana {

    @Expose
    @ConfigOption(name = "Griffin Burrows", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean griffinBurrows = false;

    @Expose
    @ConfigOption(name = "Soopy Guess", desc = "Uses §eSoopy's Guess Logic §7to find the next burrow. Does not require SoopyV2 or chat triggers to be installed.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean burrowsSoopyGuess = false;

    @Expose
    @ConfigOption(name = "Nearby Detection", desc = "Show burrows near you.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean burrowsNearbyDetection = false;

    @Expose
    @ConfigOption(name = "Smooth Transition", desc = "Show the way from one burrow to another smoothly.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean burrowSmoothTransition = false;

}
