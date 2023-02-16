package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.core.config.annotations.*;
import com.google.gson.annotations.Expose;

public class Garden {

    @Expose
    @ConfigOption(name = "Sky Mart", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean skyMart = false;

    @Expose
    @ConfigOption(name = "Copper Price", desc = "Show copper to coin prices inside the Sky Mart inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean skyMartCopperPrice = true;

    @Expose
    @ConfigOption(name = "Copper Price Position", desc = "")
    @ConfigEditorButton(runnableId = "skyMartCopperPrice", buttonText = "Edit")
    @ConfigAccordionId(id = 0)
    public Position skyMartCopperPricePos = new Position(44, -108, false, true);
}
