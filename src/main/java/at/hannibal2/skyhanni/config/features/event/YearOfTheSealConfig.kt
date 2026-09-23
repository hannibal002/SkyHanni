package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import io.github.notenoughupdates.moulconfig.observer.Property

class YearOfTheSealConfig {
    @Expose
    @ConfigOption(
        name = "Fishy Treat Profit",
        desc = "Shows what items to buy with your hard earned Fishy Treat.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var fishyTreatProfit: Boolean = true

    @Expose
    @ConfigLink(owner = YearOfTheSealConfig::class, field = "fishyTreatProfit")
    val fishyTreatProfitPosition: Position = Position(170, 150)

    // TODO rename to beachBallLine
    @Expose
    @ConfigOption(name = "Beach Ball Line", desc = "Shows a line for your Beach Balls thrown (Only works on normal ones, not giant).")
    @ConfigEditorBoolean
    @SearchTag("bouncy")
    @FeatureToggle
    val bouncyBallLine: Property<Boolean> = Property.of(true)

    // TODO rename to beachBallLineColor
    @Expose
    @ConfigOption(name = "Beach Ball Line Color", desc = "Color of the Beach Ball Line.")
    @ConfigEditorColour
    @SearchTag("bouncy")
    var bouncyBallLineColor: ChromaColour = ChromaColour.fromStaticRGB(255, 0, 196, 245)

    // TODO rename to beachBallLandingSpot
    @Expose
    @ConfigOption(name = "Beach Ball Landing Spot", desc = "Show the spot where the Beach Ball will land, and add a counter.")
    @ConfigEditorBoolean
    @SearchTag("bouncy")
    val bouncyBallLandingSpot: Property<Boolean> = Property.of(true)

    @Expose
    @Accordion
    @ConfigOption(name = "Beach Ball Tracker", desc = "")
    val beachBallTracker = BeachBallTrackerConfig()
}
