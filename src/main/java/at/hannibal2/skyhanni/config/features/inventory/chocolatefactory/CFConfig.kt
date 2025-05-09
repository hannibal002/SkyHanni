package at.hannibal2.skyhanni.config.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
//#if MC < 1.21
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFStats.CFStat
//#endif
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

// todo 1.21 impl needed
class CFConfig {
    @Expose
    @ConfigOption(name = "Chocolate Factory Features", desc = "Global toggle for all chocolate factory features.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Chocolate Factory Stats", desc = "Show general info about your chocolate factory.")
    @ConfigEditorBoolean
    @FeatureToggle
    var statsDisplay: Boolean = true

    //#if MC < 1.21
    @Expose
    @ConfigOption(
        name = "Stats List",
        desc = "Drag text to change what displays in the chocolate factory stats list and what order the text appears in.",
    )
    @ConfigEditorDraggableList
    var statsDisplayList: MutableList<CFStat> = mutableListOf(
        CFStat.HEADER,
        CFStat.CURRENT,
        CFStat.THIS_PRESTIGE,
        CFStat.ALL_TIME,
        CFStat.TIME_TO_PRESTIGE,
        CFStat.EMPTY,
        CFStat.PER_SECOND,
        CFStat.PER_MINUTE,
        CFStat.PER_HOUR,
        CFStat.PER_DAY,
        CFStat.EMPTY_2,
        CFStat.MULTIPLIER,
        CFStat.BARN,
        CFStat.TIME_TOWER,
        CFStat.TIME_TOWER_FULL,
        CFStat.LEADERBOARD_POS,
        CFStat.TIME_TO_BEST_UPGRADE,
    )
    //#endif

    @Expose
    @ConfigOption(name = "Stray Rabbit Warning", desc = "")
    @Accordion
    var rabbitWarning: CFStrayRabbitWarningConfig = CFStrayRabbitWarningConfig()

    @Expose
    @ConfigOption(name = "Upgrade Warnings", desc = "")
    @Accordion
    var chocolateUpgradeWarnings: CFUpgradeWarningsConfig = CFUpgradeWarningsConfig()

    @Expose
    @ConfigOption(name = "Chocolate Shop Price", desc = "")
    @Accordion
    var chocolateShopPrice: CFShopPriceConfig = CFShopPriceConfig()

    @Expose
    @ConfigOption(name = "Chocolate Factory Keybinds", desc = "")
    @Accordion
    var keybinds: CFKeybindsConfig = CFKeybindsConfig()

    @Expose
    @ConfigOption(name = "Chocolate Factory Custom Reminder", desc = "")
    @Accordion
    var customReminder: CFCustomReminderConfig = CFCustomReminderConfig()

    @Expose
    @ConfigOption(name = "Hoppity Collection Stats", desc = "")
    @Accordion
    var hoppityCollectionStats: HoppityCollectionStatsConfig = HoppityCollectionStatsConfig()

    @Expose
    @ConfigOption(
        name = "Show Stack Sizes",
        desc = "Show additional info as many items in the chocolate menu as the stack size.",
    )
    @ConfigEditorBoolean
    var showStackSizes: Boolean = true

    @Expose
    @ConfigOption(
        name = "Contributor Rabbit Name",
        desc = "Replaces the rabbit names in the rabbit collection menu with SkyHanni contributor names.",
    )
    @ConfigEditorBoolean
    var contributorRabbitName: Boolean = false

    @Expose
    @ConfigOption(
        name = "Highlight Upgrades",
        desc = "Highlight any upgrades that you can afford.\n" +
            "The upgrade with a star is the most optimal and the lightest color of green is the most optimal you can afford.",
    )
    @ConfigEditorBoolean
    var highlightUpgrades: Boolean = true

    @Expose
    @ConfigOption(name = "Use Middle Click", desc = "Click on slots with middle click to speed up interactions.")
    @ConfigEditorBoolean
    var useMiddleClick: Boolean = true

    @Expose
    @ConfigOption(
        name = "Rabbit Crush Threshold",
        desc = "How close should you be to your barn capacity before being warned about needing to upgrade it.",
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 20f, minStep = 1f)
    var barnCapacityThreshold: Int = 6

    @Expose
    @ConfigOption(
        name = "Rabbit Crush During Hoppity",
        desc = "Only warn about rabbit crush when the Hoppity event is active.",
    )
    @ConfigEditorBoolean
    var rabbitCrushOnlyDuringHoppity: Boolean = false

    @Expose
    @ConfigOption(name = "Extra Tooltip Stats", desc = "Show extra information about upgrades in the tooltip.")
    @ConfigEditorBoolean
    @FeatureToggle
    var extraTooltipStats: Boolean = true

    @Expose
    @ConfigOption(
        name = "Duplicate Rabbit Time",
        desc = "Show the production time of chocolate gained from duplicate rabbits.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showDuplicateTime: Boolean = false

    @Expose
    @ConfigOption(name = "Stray Rabbit Time", desc = "Show the production time of chocolate gained from stray rabbits.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showStrayTime: Boolean = false

    @Expose
    @ConfigOption(
        name = "Time Tower Usage Warning",
        desc = "Notify when you have a new time tower usage available and " +
            "continuously warn when your time tower is full.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var timeTowerWarning: Boolean = false

    @Expose
    @ConfigOption(
        name = "Time Tower Expiry Reminder",
        desc = "Notify when the time tower ends and you have one or more remaining charges.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var timeTowerReminder: Boolean = true

    @Expose
    @ConfigLink(owner = CFConfig::class, field = "statsDisplay")
    var position: Position = Position(163, 160)

    @Expose
    @ConfigOption(name = "Compact On Click", desc = "Compact the item tooltip when clicking on the chocolate.")
    @ConfigEditorBoolean
    @FeatureToggle
    var compactOnClick: Boolean = true

    @Expose
    @ConfigOption(
        name = "Always Compact",
        desc = "Always compact the item tooltip on the chocolate. Requires the above option to be enabled.",
    )
    @ConfigEditorBoolean
    var compactOnClickAlways: Boolean = false

    @Expose
    @ConfigOption(
        name = "Tooltip Move",
        desc = "Move tooltip away from the item you hover over while inside the Chocolate Factory.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var tooltipMove: Boolean = false

    @Expose
    @ConfigLink(owner = CFConfig::class, field = "tooltipMove")
    var tooltipMovePosition: Position = Position(-380, 150)

    @Expose
    @ConfigOption(
        name = "Leaderboard Change",
        desc = "Show the change of your chocolate leaderboard over time in chat.\n" +
            "This updates every time you first open the §e/cf §7menu on a new server.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var leaderboardChange: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hoppity Menu Shortcut",
        desc = "Add a Chocolate Factory button in the SkyBlock Menu that runs §e/chocolatefactory §7on click.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hoppityMenuShortcut: Boolean = true

    @Expose
    @ConfigOption(name = "Mythic Rabbit", desc = "Blocks running /cf without a §d§lMythic Rabbit Pet §7equipped.")
    @ConfigEditorBoolean
    @FeatureToggle
    var mythicRabbitRequirement: Boolean = false

    @Expose
    @ConfigOption(name = "Booster Cookie", desc = "Blocks running /cf without a §6§lBooster Cookie §7active.")
    @ConfigEditorBoolean
    @FeatureToggle
    var boosterCookieRequirement: Boolean = false

    @Expose
    @ConfigOption(name = "Hot Chocolate Mixin", desc = "Blocks running /cf without §9Hot Chocolate Mixin §7active.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hotChocolateMixinRequirement: Boolean = false

    @Expose
    @ConfigOption(name = "Stray Tracker", desc = "Track stray rabbits found in the Chocolate Factory menu.")
    @ConfigEditorBoolean
    @FeatureToggle
    var strayRabbitTracker: Boolean = true

    @Expose
    @ConfigLink(owner = CFConfig::class, field = "strayRabbitTracker")
    var strayRabbitTrackerPosition: Position = Position(300, 300)

    @Expose
    @ConfigOption(name = "Hitman Costs", desc = "Show the sum cost of remaining hitman slots.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hitmanCosts: Boolean = true

    @Expose
    @ConfigLink(owner = CFConfig::class, field = "hitmanCosts")
    var hitmanCostsPosition: Position = Position(300, 300)

    @Expose
    @ConfigOption(
        name = "§6CF §zParty Mode",
        desc = "Don't turn this on.\n§cRequires SkyHanni Chroma to be enabled to fully function.",
    )
    @ConfigEditorBoolean
    var partyMode: Property<Boolean> = Property.of(false)
}
