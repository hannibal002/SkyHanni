package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.TablistFooterUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.contains
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.startsWith
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component

// heavily inspired by SBA code
@SkyHanniModule
object TabListReader {

    private val config get() = SkyHanniMod.feature.gui.compactTabList
    private val patternGroup = RepoPattern.group("misc.compacttablist")

    var hypixelAdvertisingString = "HYPIXEL.NET"
    var renderColumns = mutableListOf<RenderColumn>()
        private set

    private var lastTab: List<Component>? = null
    private var lastFooter: Component? = null

    private var inUpgrades = false

    /**
     * REGEX-TEST: [164] CalMWolfs ᛝ♲
     * REGEX-TEST: [328] vayness ☠
     */
    val usernamePattern by patternGroup.pattern(
        "username",
        "^\\[(?<sblevel>\\d+)] (?:\\[\\w+] )?(?<username>\\w+)",
    )

    /**
     * REGEX-TEST: You have a God Potion active! 12 Hours
     */
    private val godPotPattern by patternGroup.pattern(
        "effects.godpot.colorless",
        "You have a God Potion active! (?<timer>[\\w ]+)",
    )

    /**
     * REGEX-TEST: Active Effects
     */
    private val activeEffectPattern by patternGroup.pattern(
        "effects.active.colorless",
        "Active Effects",
    )

    /**
     * REGEX-TEST: No effects active. Drink Potions or splash them on the
     * REGEX-TEST: ground to buff yourself!
     */
    private val noActiveEffectsPattern by patternGroup.pattern(
        "effects.active.none",
        "No effects active\\. Drink Potions or splash them on the|ground to buff yourself!",
    )

    /**
     * REGEX-TEST: You have 1 active effect. Use "/effects" to see it!
     * REGEX-TEST: You have 1 non-god effects.
     */
    private val effectCountPattern by patternGroup.pattern(
        "effects.count.colorless",
        "You have (?<effectCount>[0-9]+) (?:active|non-god) effects?.*",
    )

    /**
     * REGEX-TEST: Cookie Buff
     */
    private val cookiePattern by patternGroup.pattern(
        "cookie.colorless",
        "Cookie Buff",
    )

    /**
     * REGEX-TEST: Not active! Obtain booster cookies from the community
     * REGEX-TEST: shop in the hub.
     */
    private val noCookiePattern by patternGroup.pattern(
        "cookie.inactive",
        "Not active! Obtain booster cookies from the community|shop in the hub\\.",
    )

    /**
     * REGEX-TEST: Dungeon Buffs
     */
    private val dungeonBuffPattern by patternGroup.pattern(
        "dungeonbuff.colorless",
        "Dungeon Buffs",
    )

    /**
     * REGEX-TEST: Use "/effects" to see them!
     */
    private val effectsUseCommandPattern by patternGroup.pattern(
        "effects.usecommand.colorless",
        "Use \"/effects\".*",
    )

    private val upgradesHeaderPattern by patternGroup.pattern(
        "upgrades-header",
        "Upgrades",
    )

    /**
     * REGEX-TEST: Wardrobe Slots IV 5 Days
     */
    private val upgradesPattern by patternGroup.pattern(
        "upgrades",
        "(?<firstPart>[A-Za-z ]+)(?<secondPart> [\\w ]+)",
    )

    private val winterPowerUpsPattern by patternGroup.pattern(
        "winterpowerups.colorless",
        "Active Power Ups",
    )

    @HandleEvent
    fun onConfigLoad() {
        ConditionalUtils.onToggle(config.enabled) {
            rebuildRenderColumns()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabListUpdate(event: TabListUpdateEvent) {
        lastTab = event.tabList
        rebuildRenderColumns()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabListFooterUpdate(event: TablistFooterUpdateEvent) {
        lastFooter = event.footer
        rebuildRenderColumns()
    }

    private fun rebuildRenderColumns() {
        val tabList = lastTab ?: return
        val columns = rebuildColumns(tabList)
        parseSections(columns)

        val renderColumn = RenderColumn()
        renderColumns = mutableListOf(renderColumn)
        combineColumnsToRender(columns, renderColumn)
    }

    private fun rebuildColumns(tabList: List<Component>): List<TabColumn> = buildList {
        if (tabList.isNotEmpty()) {
            addAll(parseComponentColumns(tabList))
        }

        parseFooterAsColumn()?.let { add(it) }
    }

    private fun parseComponentColumns(tabList: List<Component>) = buildList {
        val fullTabComponents = AdvancedPlayerList.newSorting(tabList)

        for (entry in fullTabComponents.indices step 20) {
            val titleComponent = fullTabComponents[entry]
            val trimmedTitle = Component.literal(titleComponent.formattedTextCompat().trim())
            val column = getColumnFromComponent(this, trimmedTitle) ?: TabColumn(trimmedTitle).also {
                add(it)
            }

            for (columnEntry in (entry + 1) until fullTabComponents.size.coerceAtMost(entry + 20)) {
                column.addComponent(fullTabComponents[columnEntry])
            }
        }
    }

    // TODO refactor
    @Suppress("CyclomaticComplexMethod")
    private fun TabColumn.matchFooterTabComponent(
        component: Component,
        previousComponent: Component?,
        godPotTimer: String?,
        effectCount: Int,
    ): TabColumn = apply {
        val lastIsDungeons = dungeonBuffPattern.matches(previousComponent?.string)
        val lastIsWinterPowerUps = winterPowerUpsPattern.matches(previousComponent?.string)

        if (component.contains(hypixelAdvertisingString)) return@apply

        // These lines were consumed into the active effects header — skip them
        if (godPotTimer != null && godPotPattern.matches(component)) return@apply
        if (effectCountPattern.matches(component)) return@apply
        if (effectsUseCommandPattern.matches(component)) return@apply
        activeEffectPattern.matchMatcher(component) {
            if (godPotTimer != null) {
                addComponent(Component.literal("§a§lActive Effects:"))
                addComponent(Component.literal(" §cGod Potion§r: $godPotTimer"))
            } else {
                addComponent(Component.literal("§a§lActive Effects: §e$effectCount"))
            }
            return@apply
        }
        if (noActiveEffectsPattern.matches(component)) {
            // No need to add this, it's already implied by the 0 count
            return@apply
        }

        // For these three, the component itself is already correct — no reconstruction needed
        cookiePattern.matchMatcher(component) {
            return@apply addComponent(component)
        }
        if (noCookiePattern.matches(component)) {
            if (noCookiePattern.matches(previousComponent)) return@apply
            return@apply addComponent(Component.literal("§7 Not Active"))
        }

        dungeonBuffPattern.matchMatcher(component) {
            return@apply addComponent(component)
        }
        if (lastIsDungeons && component.startsWith("No Buffs active.")) {
            return@apply addComponent(Component.literal("§7 None Found"))
        }

        winterPowerUpsPattern.matchMatcher(component) {
            return@apply addComponent(component)
        }
        if (lastIsWinterPowerUps && component.startsWith("No Power Ups active.")) {
            return@apply addComponent(Component.literal("§7 None"))
        }

        if (upgradesHeaderPattern.matches(component)) {
            inUpgrades = true
        }
        upgradesPattern.matchMatcher(component) {
            if (!inUpgrades) return@matchMatcher
            if (!component.formattedTextCompat().startsWith("§e")) return@matchMatcher

            val firstComponent = TextHelper.matcher(component, group("firstPart")) ?: return@apply
            val secondComponent = TextHelper.matcher(component, group("secondPart")) ?: return@apply
            val displayFirst = if (!firstComponent.startsWith(" ") && !firstComponent.style.isBold) {
                TextHelper.join(" ", firstComponent)
            } else firstComponent

            addComponent(displayFirst)
            addComponent(secondComponent)
            return@apply
        }

        val formatted = component.formattedTextCompat()
        when {
            // Separators are truly emptied
            formatted.removeColor().trim().isEmpty() -> {
                addComponent(Component.empty())
                inUpgrades = false
            }

            "§l" !in formatted -> addComponent(Component.literal(" ").append(component))
            else -> addComponent(component)
        }
    }

    // TODO refactor
    @Suppress("CyclomaticComplexMethod")
    private fun parseFooterAsColumn(): TabColumn? {
        val component = lastFooter ?: return null
        inUpgrades = false

        val lines = TextHelper.split(component, "\n") ?: listOf(component)

        val godPotTimer = lines.firstNotNullOfOrNull {
            godPotPattern.matchMatcher(it.string) { group("timer") }
        }
        val effectCount = lines.firstNotNullOfOrNull {
            effectCountPattern.matchMatcher(it.string) { group("effectCount").toInt() }
        } ?: 0

        val titleColumn = Component.literal("§2§lOther")
        return TabColumn(titleColumn).apply {
            for ((index, lineComponent) in lines.withIndex()) {
                val previousComponent = lines.getOrNull(index - 1)
                matchFooterTabComponent(lineComponent, previousComponent, godPotTimer, effectCount)
            }
            while (components.isNotEmpty() && components.last().string.trim().isEmpty()) {
                removeLastComponent()
            }
        }.takeIf { it.components.isNotEmpty() }
    }

    private fun getColumnFromComponent(columns: List<TabColumn>, component: Component): TabColumn? =
        columns.find { component == it.titleComponent }

    private fun parseSections(columns: List<TabColumn>) {
        for (column in columns) {
            var currentTabSection: TabSection? = null
            for (line in column.components) {
                if (line.string.isEmpty()) {
                    currentTabSection = null
                    continue
                }

                if (currentTabSection == null) {
                    column.addSection(TabSection(column).also { currentTabSection = it })
                }

                currentTabSection?.addComponent(line)
            }
        }
    }

    private fun combineColumnsToRender(columns: List<TabColumn>, firstColumn: RenderColumn) {
        var currentColumn = firstColumn
        var lastTitleComponent: Component? = null

        fun newColumnOrSpacer(required: Boolean) {
            if (required || currentColumn.size() >= TabListRenderer.MAX_LINES) {
                renderColumns.add(RenderColumn().also { currentColumn = it })
            } else if (currentColumn.size() > 0) {
                currentColumn.addLine(AdvancedPlayerList.createTabLine(Component.literal(""), TabStringType.TEXT))
            }
        }

        fun addLine(line: Component) {
            if (currentColumn.size() >= TabListRenderer.MAX_LINES) {
                renderColumns.add(RenderColumn().also { currentColumn = it })
            }
            currentColumn.addLine(AdvancedPlayerList.createTabLine(line, TabStringType.fromComponent(line)))
        }

        for (section in columns.flatMap { it.sections }) {
            val needsTitle = lastTitleComponent != section.columnValue.titleComponent
            val sectionSize = section.size() + if (needsTitle) 1 else 0
            val isLarge = sectionSize >= TabListRenderer.MAX_LINES / 2

            newColumnOrSpacer(required = isLarge && currentColumn.size() >= TabListRenderer.MAX_LINES)

            if (needsTitle) {
                lastTitleComponent = section.columnValue.titleComponent
                currentColumn.addLine(AdvancedPlayerList.createTabLine(lastTitleComponent, TabStringType.TITLE))
            }

            for (line in section.components) addLine(line)
        }
    }
}
