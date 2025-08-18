package at.hannibal2.skyhanni.config.features.itemability

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
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
    @ConfigOption(name = "Crown Coins Per Hour", desc = "Show coins per hour in the Avarice Counter.")
    @ConfigEditorBoolean
    var perHour: Boolean = false

    @Expose
    @ConfigOption(name = "Time until Max Crown", desc = "Shows the time until you reach max coins (1B coins).")
    @ConfigEditorBoolean
    var time: Boolean = false

    @Expose
    @ConfigOption(name = "Last coins gained", desc = "Shows the amount of scavenger coins gained by last killed mob.")
    @ConfigEditorBoolean
    var coinDiff: Boolean = false

    @Expose
    @ConfigOption(name = "Coins this session", desc = "Shows the amount of coins you gained in a session.")
    @ConfigEditorBoolean
    var coinsSession = false

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
    @ConfigOption(name = "Session Time", desc = "Shows the elapsed time since session start.")
    @ConfigEditorBoolean
    var sessionTime: Boolean = false


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
