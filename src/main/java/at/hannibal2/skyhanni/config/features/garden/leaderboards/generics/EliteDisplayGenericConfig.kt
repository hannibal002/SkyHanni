package at.hannibal2.skyhanni.config.features.garden.leaderboards.generics

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

open class EliteDisplayGenericConfig{
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
    @ConfigLink(owner = EliteDisplayGenericConfig::class, field = "display")
    val pos: Position = Position(180, 10)

    @Expose
    @ConfigOption(
        name = "Text",
        desc = "Drag text to change the appearance of the overlay.\n"
    )
    @ConfigEditorDraggableList
    val text: Property<MutableList<TextEntry>> = Property.of(mutableListOf(
        TextEntry.WEIGHT_POSITION,
        TextEntry.OVERTAKE
    ))

    @Expose
    @ConfigOption(
        name = "Leaderboard Ranking",
        desc = "Show your position in the farming weight leaderboard. " +
            "Only if your farming weight is high enough! Updates periodically."
    )
    @ConfigEditorBoolean
    val leaderboard: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Show Outside Garden", desc = "Show your pest kills outside of the garden.")
    @ConfigEditorBoolean
    var showOutsideGarden: Boolean = false


    enum class TextEntry(private val displayName: String) {
        WEIGHT_POSITION("§6Leaderboard: §eAmount §7[§b#Rank§7]"),
        OVERTAKE("§eAmount §7(§bTime§7) §7behind §bPlayer"),
        LAST_PLAYER("§eAmount §7ahead of §bPlayer")
        ;

        override fun toString() = displayName
    }
}
