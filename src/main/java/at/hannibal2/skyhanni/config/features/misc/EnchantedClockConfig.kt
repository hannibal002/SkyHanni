package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.features.misc.EnchantedClockHelper.SimpleBoostType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class EnchantedClockConfig {
    @Expose
    @ConfigOption(
        name = "Enchanted Clock Reminder",
        desc = "Show reminders when an Enchanted Clock charge for a boost type is available."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var reminder: Boolean = true

    @Expose
    @ConfigOption(name = "Reminder Boosts", desc = "List of boost types to remind about.")
    @ConfigEditorDraggableList
    val reminderBoosts: MutableList<SimpleBoostType> = mutableListOf(
        SimpleBoostType.MINIONS,
        SimpleBoostType.CHOCOLATE_FACTORY,
        SimpleBoostType.PET_TRAINING,
        SimpleBoostType.PET_SITTER,
        SimpleBoostType.AGING_ITEMS,
        SimpleBoostType.FORGE
    )

    @Expose
    @ConfigOption(
        name = "Repeat Reminder",
        desc = "Repeat reminders every §cX §7minutes until you use the boost.\n" +
            "§eSet to 0 to disable."
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 60f, minStep = 1f)
    var repeatReminder: Int = 0
}
