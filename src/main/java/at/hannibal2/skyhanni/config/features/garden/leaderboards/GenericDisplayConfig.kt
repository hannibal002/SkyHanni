package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

open class GenericDisplayConfig {
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
    @ConfigLink(owner = FarmingWeightDisplayConfig::class, field = "display")
    val pos: Position = Position(180, 10)

    @Expose
    @ConfigOption(
        name = "Leaderboard Ranking",
        desc = "Show your position on the leaderboard. " +
            "Only shows if you are ranked on that leaderboard. Updates periodically"
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
    @ConfigOption(name = "Show Outside Garden", desc = "Show the farming weight outside of the garden.")
    @ConfigEditorBoolean
    var showOutsideGarden: Boolean = false
}
