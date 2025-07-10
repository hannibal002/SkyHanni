package at.hannibal2.skyhanni.config.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CFCustomReminderConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show a custom reminder until you can purchase the next upgrade.\n" +
            "Click on one item you cant buy to select/deselect it."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Always Custom Reminder",
        desc = "Always show the display, even outside the chocolate factory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var always: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hide No Chocolate Message",
        desc = "Hide the chat message about not having enough chocolate to buy/purchase something."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideChat: Boolean = true

    @Expose
    @ConfigLink(owner = CFConfig::class, field = "customReminder")
    val position: Position = Position(390, 90, 1f, true)
}
