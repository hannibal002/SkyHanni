package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class PestKillsDisplayConfig {
    @Expose
    @ConfigOption(
        name = "Display",
        desc = "Display your farming weight on screen.\n" +
            "The calculation and API is provided by The Elite SkyBlock farmers.\n" +
            "See §eelitebot.dev/info §7for more info."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var display: Boolean = true

    @Expose
    @ConfigLink(owner = PestKillsDisplayConfig::class, field = "display")
    val pos: Position = Position(180, 10)

    @Expose
    @ConfigOption(
        name = "Farming Weight Text",
        desc = "Drag text to change the appearance of the overlay.\n"
    )
    @ConfigEditorDraggableList
    val text: Property<MutableList<PestKillsTextEntry>> = Property.of(
        mutableListOf(
            PestKillsTextEntry.WEIGHT_POSITION,
            PestKillsTextEntry.OVERTAKE
        )
    )

    enum class PestKillsTextEntry(private val displayName: String) {
        WEIGHT_POSITION("§6Farming Weight: §e104,481.49 §7[§b#5§7]"),
        OVERTAKE("§e170.21 §7(§b12h 32m 15s§7) §7behind §bChissl")
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Leaderboard Ranking",
        desc = "Show your position in the farming weight leaderboard. " +
            "Only if your farming weight is high enough! Updates periodically."
    )
    @ConfigEditorBoolean
    val leaderboard: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Overtake ETA",
        desc = "Show a timer estimating when you'll move up a spot in the leaderboard! " +
            "Does not factor in pests or dicer drops. Garden Milestones Display must be enabled."
    )
    @ConfigEditorBoolean
    val overtakeETA: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Show LB Change",
        desc = "Show the change of your position in the farming weight leaderboard while you were offline."
    )
    @ConfigEditorBoolean
    var showLbChange: Boolean = false

    @Expose
    @ConfigOption(name = "Always ETA", desc = "Show the Overtake ETA always, even when not farming at the moment.")
    @ConfigEditorBoolean
    val overtakeETAAlways: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Use ETA Goal",
        desc = "Use the ETA Goal number instead of the next upcoming rank. Useful when your rank is in the " +
            "ten thousands and you don't want to see small ETAs."
    )
    @ConfigEditorBoolean
    val useEtaGoalRank: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "ETA Goal",
        desc = "Override the Overtake ETA to show when you'll reach the specified rank (if not there yet). (Default: \"10,000\")"
    )
    @ConfigEditorText
    val etaGoalRank: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Show below 200", desc = "Show the farming weight data even if you are below 200 weight.")
    @ConfigEditorBoolean
    var ignoreLow: Boolean = false

    @Expose
    @ConfigOption(name = "Show Outside Garden", desc = "Show the farming weight outside of the garden.")
    @ConfigEditorBoolean
    var showOutsideGarden: Boolean = false
}
