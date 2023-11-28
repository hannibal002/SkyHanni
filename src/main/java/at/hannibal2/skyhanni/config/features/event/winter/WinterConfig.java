package at.hannibal2.skyhanni.config.features.event.winter;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.features.event.winter.frozentreasure.FrozenTreasureConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class WinterConfig {

    @Expose
    @ConfigOption(name = "Frozen Treasure Tracker", desc = "")
    @Accordion
    public FrozenTreasureConfig frozenTreasureTracker = new FrozenTreasureConfig();

    @Expose
    @ConfigOption(name = "Island Close Time", desc = "While on the Winter Island, show a timer until Jerry's Workshop closes.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean islandCloseTime = true;

    @Accordion
    @Expose
    @ConfigOption(name = "Unique Gifting Opportunities", desc = "Highlight players who you haven't given gifts to yet")
    public GiftingOpportunitiesConfig giftingOpportunities = new GiftingOpportunitiesConfig();

    @Expose
    public Position islandCloseTimePosition = new Position(10, 10, false, true);

}
