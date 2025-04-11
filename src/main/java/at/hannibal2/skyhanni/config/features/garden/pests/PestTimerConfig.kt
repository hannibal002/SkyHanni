package at.hannibal2.skyhanni.config.features.garden.pests

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PestTimerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the time since the last pest spawned in your garden.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Only With Farming Tool", desc = "Only show the display when holding a farming tool in hand.")
    @ConfigEditorBoolean
    var onlyWithFarmingTool: Boolean = true

    @Expose
    @ConfigOption(name = "Only With Vacuum", desc = "Only show the time while holding a vacuum in the hand.")
    @ConfigEditorBoolean
    var onlyWithVacuum: Boolean = false

    @Expose
    @ConfigOption(name = "Pest Timer Text", desc = "Drag text to change the appearance of the overlay.")
    @ConfigEditorDraggableList
    var pestDisplay: MutableList<PestTimerTextEntry> = mutableListOf(
        PestTimerTextEntry.PEST_TIMER,
        PestTimerTextEntry.PEST_COOLDOWN
    )

    enum class PestTimerTextEntry(private val displayName: String) {
        PEST_TIMER("§eLast pest spawned: §b8s ago"),
        PEST_COOLDOWN("§ePest Cooldown: §b1m 8s"),
        AVERAGE_PEST_SPAWN("§eAverage time to spawn: §b4m 32s");

        override fun toString(): String {
            return displayName
        }
    }

    @Expose
    @ConfigOption(name = "Pest Cooldown Warning", desc = "Warn when pests are eligible to spawn.")
    @ConfigEditorBoolean
    @FeatureToggle
    var cooldownOverWarning: Boolean = false

    @Expose
    @ConfigOption(name = "Warn Before Cooldown End", desc = "Warn this many seconds before the cooldown is over.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 30f, minStep = 1f)
    var cooldownWarningTime: Int = 5

    @Expose
    @ConfigOption(
        name = "AFK Timeout",
        desc = "Don't include spawn time in average spawn time display when the player goes AFK for at least this many seconds."
    )
    @ConfigEditorSlider(minValue = 5f, maxValue = 300f, minStep = 1f)
    var averagePestSpawnTimeout: Int = 30

    @Expose
    @ConfigOption(
        name = "Pest Spawn Time Chat Message",
        desc = "When a pest spawns, send the time it took to spawn it in chat."
    )
    @ConfigEditorBoolean
    var pestSpawnChatMessage: Boolean = false

    @Expose
    @ConfigLink(owner = PestTimerConfig::class, field = "enabled")
    var position: Position = Position(383, 93, false, true)
}
