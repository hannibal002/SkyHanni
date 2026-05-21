package at.hannibal2.skyhanni.config.features.itemability

import at.hannibal2.skyhanni.features.itemabilities.abilitycooldown.ItemAbility
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import kotlin.time.Duration

class AbilityCooldownNotificationConfig {
    @Expose
    @ConfigOption(
        name = "Ability Cooldown Notifications",
        desc = "Select abilities to trigger title notifications when their cooldowns reach the threshold.",
    )
    @ConfigEditorDraggableList
    val enabledAbilities: MutableList<ItemAbility> = mutableListOf()

    @Expose
    @ConfigOption(
        name = "Notification Threshold",
        desc = "Seconds before cooldown is ready to trigger title for all selected abilities.",
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 30f, minStep = 1f)
    val notificationThreshold: Int = 0

    @Expose
    @ConfigOption(
        name = "Title Duration",
        desc = "Duration the title notification is shown when an ability cooldown reaches the threshold.",
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 5f, minStep = 0.5f)
    val titleDuration: Float = 3f
}
