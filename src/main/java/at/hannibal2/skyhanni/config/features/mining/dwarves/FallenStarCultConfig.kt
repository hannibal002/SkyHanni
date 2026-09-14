package at.hannibal2.skyhanni.config.features.mining.dwarves

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FallenStarCultConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show the time until the next Cult of the Fallen Star meeting in the Dwarven Mines.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Only Show When",
        desc = "Only show the timer in these situations.\nLeave empty to always show.",
    )
    @ConfigEditorDraggableList
    val onlyShowWhen: MutableList<ShowCondition> = mutableListOf(
        ShowCondition.IN_CULT_ROOM,
        ShowCondition.WEARING_HELMET,
    )

    enum class ShowCondition(private val displayName: String) {
        IN_CULT_ROOM("In Cult Room"),
        WEARING_HELMET("Wearing Fallen Star Helmet"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigLink(owner = FallenStarCultConfig::class, field = "enabled")
    val position: Position = Position(-400, 200)
}
