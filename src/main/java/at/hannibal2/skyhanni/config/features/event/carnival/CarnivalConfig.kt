package at.hannibal2.skyhanni.config.features.event.carnival

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CarnivalConfig {
    @Expose
    @ConfigOption(name = "Zombie Shootout", desc = "")
    @Accordion
    val zombieShootout: ZombieShootoutConfig = ZombieShootoutConfig()

    @Expose
    @ConfigOption(
        name = "Reminder Daily Tickets",
        desc = "Reminds you when tickets can be claimed from the carnival leader."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var reminderDailyTickets: Boolean = true

    @Expose
    @ConfigOption(name = "Show Goals", desc = "Displays the goals for this carnival event.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showGoals: Boolean = true

    @Expose
    @ConfigLink(owner = CarnivalConfig::class, field = "showGoals")
    val goalsPosition: Position = Position(20, 20)

    @Expose
    @ConfigOption(
        name = "Double Click to Start",
        desc = "Clicking the npc again after the npc finishes talking to start game."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var doubleClickToStart: Boolean = true

    @Expose
    @ConfigOption(name = "Token Shop Helper", desc = "Show extra information about remaining upgrades in Event Shops.")
    @ConfigEditorBoolean
    @FeatureToggle
    var tokenShopHelper: Boolean = true
}
