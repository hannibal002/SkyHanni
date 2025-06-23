package at.hannibal2.skyhanni.config.features.event.hoppity.summary

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class HoppityEventSummaryConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a summary of your Hoppity's Hunt stats when the event is over.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @Accordion
    @ConfigOption(name = "Live Display", desc = "")
    var liveDisplay: HoppityLiveDisplayConfig = HoppityLiveDisplayConfig()

    @Expose
    @Accordion
    @ConfigOption(name = "Update CF Reminders", desc = "")
    var cfReminder: HoppityEventSummaryCFReminderConfig = HoppityEventSummaryCFReminderConfig()

    @Expose
    @ConfigLink(owner = HoppityEventSummaryConfig::class, field = "liveDisplay")
    var liveDisplayPosition: Position = Position(0, 0)

    @Expose
    @ConfigOption(name = "Stats List", desc = "Drag text to change what displays in the summary card.")
    @ConfigEditorDraggableList
    var statDisplayList: Property<MutableList<HoppityStat>> = Property.of(
        mutableListOf(
            HoppityStat.MEAL_EGGS_FOUND,
            HoppityStat.HOPPITY_RABBITS_BOUGHT,
            HoppityStat.SIDE_DISH_EGGS,
            HoppityStat.MILESTONE_RABBITS,
            HoppityStat.EMPTY_1,
            HoppityStat.NEW_RABBITS,
            HoppityStat.EMPTY_2,
            HoppityStat.DUPLICATE_RABBITS,
            HoppityStat.EMPTY_3,
            HoppityStat.TIME_IN_CF,
            HoppityStat.RABBIT_THE_FISH_FINDS,
            HoppityStat.LEADERBOARD_CHANGE,
        ),
    )

    @Suppress("MaxLineLength")
    enum class HoppityStat(private val displayName: String) {
        MEAL_EGGS_FOUND("§7You found §b45§7/§a47 §6Chocolate Meal Eggs§7."),
        HITMAN_EGGS("§7You recovered §b20§7/§a40 §7missed §6Meal Eggs §7from §cRabbit Hitman§7."),
        HOPPITY_RABBITS_BOUGHT("§7You bought §b7 §fRabbits §7from §aHoppity§7."),
        SIDE_DISH_EGGS("§7You found §b4 §6§lSide Dish Eggs §r§7in the §6Chocolate Factory§7."),
        MILESTONE_RABBITS("§7You claimed §b2 §6§lMilestone Rabbits§7."),
        EMPTY_1(""),
        NEW_RABBITS("§7Unique Rabbits: §f7\n §f1 §7- §a1 §7- §91 §7- §51 §7- §61 §7- §d1 §7- §b1"),
        EMPTY_2(""),
        DUPLICATE_RABBITS("§7Duplicate Rabbits: §f10\n §f4 §7- §a3 §7- §92 §7- §51 §7- §60 §7- §d0 §7- §b0\n §6+250,000,000 Chocolate"),
        EMPTY_3(""),
        STRAY_RABBITS("§7Stray Rabbits: §f20\n §f10 §7- §a6 §7- §93 §7- §51 §7- §60 §7- §d0 §7- §b0\n §6+8,000,000 Chocolate\n  §4* §c§oRequires Stray Tracker being enabled to work§4§o."),
        EMPTY_4(""),
        TIME_IN_CF("§7You spent §b4h 36m §7in the §6Chocolate Factory§7."),
        RABBIT_THE_FISH_FINDS("§7You found §cRabbit the Fish §7in Meal Eggs §b2 §7times."),
        LEADERBOARD_CHANGE("§7Leaderboard: §b#107 §c-> §b#99\n §7(§a+8 Spots§7) Top §a0.003% §c-> §7Top §a0.002%\n§4* §c§oSyncs on first §4§o/cf §c§orun per server swap§4§o.");

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Show Count Diff",
        desc = "Store and display the count of Uniques/Duplicates next to their respective rabbit count.\n" +
            ".e.g. §7Unique Rabbits: §f5 §7(270 -> 275)",
    )
    @ConfigEditorBoolean
    var showCountDiff: Boolean = false
}
