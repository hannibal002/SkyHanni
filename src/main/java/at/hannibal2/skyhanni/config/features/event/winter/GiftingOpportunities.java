package at.hannibal2.skyhanni.config.features.event.winter;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class GiftingOpportunities {
    @Expose
    @ConfigOption(name = "Highlight unique gift opportunities", desc = "Highlight players who you haven't given gifts to yet")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightSpecialNeedsPlayers = true;

    @Expose
    @ConfigOption(name = "Display only while holding a gift", desc = "Only highlight ungifted players while holding a gift.")
    @ConfigEditorBoolean
    public boolean highlighWithGiftOnly = true;


    @Expose
    @ConfigOption(name = "Use armor stands", desc = "Make use of armor stands to stop highlighting players. A bit inaccurate, but can help with people you gifted before this feature was used.")
    @ConfigEditorBoolean
    public boolean useArmorStandDetection = false;

}
