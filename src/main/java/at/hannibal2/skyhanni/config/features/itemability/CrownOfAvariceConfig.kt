package at.hannibal2.skyhanni.config.features.itemability

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CrownOfAvariceConfig {
    @Expose
    @ConfigOption(name = "Counter", desc = "Shows the current coins of your crown of avarice (if worn).")
    @ConfigEditorBoolean
    @FeatureToggle
    var enable: Boolean = false

    @Expose
    @ConfigOption(
        name = "Counter format",
        desc = "Have the crown of avarice counter as short format instead of every digit.",
    )
    @ConfigEditorBoolean
    var shortFormat: Boolean = true

    @Expose
    @ConfigOption(name = "Coins Per Hour format", desc = "Shows the coins per hour gained as short format i.e. 7.3M.")
    @ConfigEditorBoolean
    var shortFormatCPH: Boolean = true

    @Expose
    @ConfigOption(
        name = "Afk Pause Time",
        desc = "Pauses the timer if no coins are added after this amount of time in seconds."
    )
    @ConfigEditorSlider(minValue = 5F, maxValue = 180F, minStep = 5F)
    var afkTimeout: Int = 120

    @Expose
    @ConfigOption(
        name = "Session Active Timer",
        desc = "Waits the duration (in seconds) before session statistics are displayed after loading in."
    )
    @ConfigEditorSlider(minValue = 0F, maxValue = 10F, minStep = 1F)
    var sessionActiveTime: Int = 10

    @Expose
    @ConfigOption(name = "Reset on World Change", desc = "Resets your session on world change if enabled.")
    @ConfigEditorBoolean
    var resetOnWorldChange = false

    @Expose
    @ConfigOption(name = "Tracker Text", desc = "Drag the text to change the appearance of the overlay.")
    @ConfigEditorDraggableList
    val text: MutableList<CrownOfAvariceLines> = mutableListOf(
        CrownOfAvariceLines.COINSPERHOUR,
        CrownOfAvariceLines.TIMEUNTILMAX,
        CrownOfAvariceLines.COINDIFFERENCE,
        CrownOfAvariceLines.SESSIONCOINS,
        CrownOfAvariceLines.SESSIONTIME,
    )


    enum class CrownOfAvariceLines(private val displayName: String) {
        COINSPERHOUR("§aCoins Per Hour: §61,234,567 / 1.23M"),
        TIMEUNTILMAX("§aTime until Max: §61234y 56d 7h 8m 9s"),
        COINDIFFERENCE("§aLast coins gained: §61234"),
        SESSIONCOINS("§aCoins this session: §6123,456,789"),
        SESSIONTIME("§aSession Time: §612m 34s"),
        ;override fun toString() = displayName
    }



    @Expose
    @ConfigLink(owner = CrownOfAvariceConfig::class, field = "enable")
    val position: Position = Position(20, 20)
}
