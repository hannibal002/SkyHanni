package at.hannibal2.skyhanni.config.features.itemability

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.itemabilities.abilitycooldown.ItemAbility
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ItemAbilityCooldownNotificationConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show title notifications when selected item abilities' cooldowns reach the threshold.",
    )
    @ConfigEditorBoolean
    var enabled: Boolean = false

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

    @Expose
    @ConfigOption(
        name = "Sound Type",
        desc = "Select which sound to play when an ability cooldown reaches the threshold.",
    )
    @ConfigEditorDropdown
    val soundType: NotificationSound = NotificationSound.PLING

    @Expose
    @ConfigOption(
        name = "Ready Message",
        desc = "Message displayed when the ability is ready.",
    )
    @ConfigEditorText
    val readyMessage: String = "&a&l{ability}&r &ais ready"

    @Expose
    @ConfigOption(
        name = "Soon Message",
        desc = "Message displayed when the ability is nearly ready.",
    )
    @ConfigEditorText
    val soonMessage: String = "&e&l{ability}&r &6in {time}s"

    @Expose
    @ConfigLink(owner = ItemAbilityCooldownNotificationConfig::class, field = "enabled")
    val position: Position = Position(-475, -240, 3.4f, true)

    enum class NotificationSound(private val displayName: String) {
        None("None"),
        PLING("Pling"),
        CLICK("Click"),
        BEEP("Beep"),
        ERROR("Error"),
        ;

        override fun toString() = displayName
    }
}
