package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.garden.CropType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CropCollectionDisplayConfig {
    @Expose
    @ConfigOption(
        name = "Display",
        desc = "Display your farming weight on screen.\n" +
            "The calculation and API is provided by The Elite SkyBlock farmers.\n" +
            "See §eelitebot.dev/info §7for more info."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var display: Boolean = true // TODO config fix this based on milestones display

    // TODO goal rank for each crop
    @Expose
    @ConfigLink(owner = CropCollectionDisplayConfig::class, field = "display")
    val pos: Position = Position(180, 10)

    @Expose
    @ConfigOption(name = "Hide When Not Farming", desc = "Hides the display unless actively farming.")
    @ConfigEditorBoolean
    val hideWhenNotFarming: Boolean = true

    @Expose
    @ConfigOption(
        name = "Crop Collection Text",
        desc = "Drag text to change the appearance of the overlay.\n"
    )
    @ConfigEditorDraggableList
    val text: Property<MutableList<CropCollectionTextEntry>> = Property.of(
        mutableListOf(
            CropCollectionTextEntry.WEIGHT_POSITION,
            CropCollectionTextEntry.OVERTAKE
        )
    )

    // TODO fix when no longer updating display
    enum class CropCollectionTextEntry(private val displayName: String) {
        WEIGHT_POSITION("§6Farming Weight: §e104,481.49 §7[§b#5§7]"),
        OVERTAKE("§e170.21 §7(§b12h 32m 15s§7) §7behind §bChissl"),
        LAST_PLAYER("§e170.21 §7 §7ahead of §bChissl")
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
    @ConfigOption(name = "Always ETA", desc = "Show the Overtake ETA always, even when not farming at the moment.")
    @ConfigEditorBoolean
    val overtakeETAAlways: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Use Rank Goal",
        desc = "Use the Rank Goal number instead of the next upcoming rank. Useful when your rank is in the " +
            "ten thousands and you don't want to see small ETAs."
    )
    @ConfigEditorBoolean
    val useRankGoal: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Rank Goal",
        desc = "What crops to set a custom rank goal for. Applies to all leaderboard modes."
    )
    @ConfigEditorDraggableList
    val rankGoalCrops: Property<MutableList<CropType>> = Property.of(mutableListOf())

    @Expose
    @ConfigOption(
        name = "All-Time Crop Rank Goals",
        desc = ""
    )
    @Accordion
    val cropRankGoalsConfig: CropRankGoalsConfig = CropRankGoalsConfig()

    @Expose
    @ConfigOption(
        name = "Monthly Crop Rank Goals",
        desc = ""
    )
    @Accordion
    val monthlyCropRankGoalsConfig: CropRankGoalsConfig = CropRankGoalsConfig()

    @Expose
    @ConfigOption(
        name = "Overtake Player Message",
        desc = "Send a message when you overtake a player."
    )
    @ConfigEditorBoolean
    var overtakePlayerMessage: Boolean = false

    @Expose
    @ConfigOption(
        name = "Offline leaderboard change",
        desc = "Send a message with the change of your position in the farming weight leaderboard while you were offline."
    )
    @ConfigEditorBoolean
    var offlineChangeMessage: Boolean = false


    @Expose
    @ConfigOption(name = "Show Outside Garden", desc = "Show your crop collection outside of the garden.")
    @ConfigEditorBoolean
    var showOutsideGarden: Boolean = false
}
