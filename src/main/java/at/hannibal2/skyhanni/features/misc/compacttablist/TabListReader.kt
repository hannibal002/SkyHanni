package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
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

    private var lastTabComponents: List<Component>? = null
    private var lastFooterComponent: Component? = null

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

    /**
     * REGEX-TEST: Wardrobe Slots IV 5 Days
     */
    private val upgradesPattern by patternGroup.pattern(
        "upgrades",
        "(?<firstPart>[A-Za-z ]+)(?<secondPart> [\\w ]+)"
    )
    private val winterPowerUpsPattern by patternGroup.pattern(
        "winterpowerups.colorless",
        "Active Power Ups",
    )

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.enabled) {
            rebuildRenderColumns()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabListUpdate(event: TabListUpdateEvent) {
        this.lastTabComponents = event.tabList
        rebuildRenderColumns()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabListFooterUpdate(event: TablistFooterUpdateEvent) {
        this.lastFooterComponent = event.footer
        rebuildRenderColumns()
    }

    private fun rebuildRenderColumns() {
        if (lastTabComponents == null) return
        val columns = rebuildColumns()
        parseSections(columns)

        val renderColumn = RenderColumn()
        renderColumns = mutableListOf(renderColumn)
        combineColumnsToRender(columns, renderColumn)
    }

    private fun rebuildColumns(): MutableList<TabColumn> = buildList {
        val components = this@TabListReader.lastTabComponents ?: emptyList()
        addAll(parseComponentColumns(components))

        val footer = this@TabListReader.lastFooterComponent ?: return@buildList
        parseFooterAsColumn(footer)?.let { add(it) }
    }.toMutableList()

    private fun parseComponentColumns(components: List<Component>): MutableList<TabColumn> {
        if (components.isEmpty()) return mutableListOf()
        val columns = mutableListOf<TabColumn>()
        val fullTabComponents = AdvancedPlayerList.newSorting(components)

        for (entry in fullTabComponents.indices step 20) {
            val titleComponent = fullTabComponents[entry]
            val trimmedTitle = Component.literal(titleComponent.formattedTextCompat().trim())
            val column = getColumnFromComponent(columns, trimmedTitle) ?: TabColumn(trimmedTitle).also {
                columns.add(it)
            }

            for (columnEntry in (entry + 1) until fullTabComponents.size.coerceAtMost(entry + 20)) {
                column.addComponent(fullTabComponents[columnEntry])
            }
        }
        return columns
    }

    private fun TabColumn.matchFooterTabComponent(
        component: Component,
        previousComponent: Component?,
        godPotTimer: String?,
        effectCount: String?,
    ): TabColumn = this.apply {
        val lastIsCookieBuff = previousComponent?.string == "Cookie Buff"
        val lastIsDungeons = previousComponent?.string == "Dungeon Buffs"
        val lastIsWinterPowerUps = previousComponent?.string == "Active Power Ups"

        if (component.contains(hypixelAdvertisingString)) return@apply

        // These lines were consumed into the active effects header — skip them
        if (godPotTimer != null && godPotPattern.matches(component)) return@apply
        if (effectCountPattern.matches(component)) return@apply
        if (effectsUseCommandPattern.matches(component)) return@apply

        activeEffectPattern.matchMatcher(component) {
            when {
                godPotTimer != null -> {
                    addComponent(Component.literal("§a§lActive Effects:"))
                    addComponent(Component.literal(" §cGod Potion§r: $godPotTimer"))
                }
                effectCount != null -> addComponent(Component.literal("§a§lActive Effects: §e$effectCount"))
                else -> addComponent(Component.literal("§a§lActive Effects: §e0"))
            }
            return@apply
        }

        // For these three, the component itself is already correct — no reconstruction needed
        cookiePattern.matchMatcher(component) {
            return@apply addComponent(component)
        }
        if (component.startsWith("Not active!") && lastIsCookieBuff) {
            return@apply addComponent(Component.literal("§7 Not Active"))
        }

        dungeonBuffPattern.matchMatcher(component) {
            return@apply addComponent(component)
        }
        if (component.startsWith("No Buffs active.") && lastIsDungeons) {
            return@apply addComponent(Component.literal("§7 None Found"))
        }

        winterPowerUpsPattern.matchMatcher(component) {
            return@apply addComponent(component)
        }
        if (component.startsWith("No Power Ups active.") && lastIsWinterPowerUps) {
            return@apply addComponent(Component.literal("§7 None"))
        }

        upgradesPattern.matchMatcher(component.string) {
            if (!component.formattedTextCompat().startsWith("§e")) return@matchMatcher

            val firstComponent = TextHelper.matcher(component, group("firstPart")) ?: return@apply
            val secondComponent = TextHelper.matcher(component, group("secondPart")) ?: return@apply
            val displayFirst = if (!firstComponent.string.startsWith(" ") && !firstComponent.style.isBold) {
                TextHelper.join(" ", firstComponent)
            } else firstComponent

            addComponent(displayFirst)
            addComponent(secondComponent)
            return@apply
        }

        val formatted = component.formattedTextCompat()
        when {
            // Separators are truly emptied
            formatted.removeColor().trim().isEmpty() -> addComponent(Component.empty())
            !formatted.contains("§l") -> addComponent(Component.literal(" ").append(component))
            else -> addComponent(component)
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun parseFooterAsColumn(component: Component): TabColumn? {
        val lines = TextHelper.split(component, "\n") ?: listOf(component)

        val godPotTimer = lines.firstNotNullOfOrNull {
            godPotPattern.matchMatcher(it.string) { group("timer") }
        }
        val effectCount = lines.firstNotNullOfOrNull {
            effectCountPattern.matchMatcher(it.string) { group("effectCount") }
        }

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

    private fun getColumnFromComponent(columns: List<TabColumn>, component: Component): TabColumn? {
        for (tabColumn in columns) {
            if (component == tabColumn.titleComponent) {
                return tabColumn
            }
        }
        return null
    }

    private fun parseSections(columns: MutableList<TabColumn>) {
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

    private fun combineColumnsToRender(columns: MutableList<TabColumn>, firstColumn: RenderColumn) {
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
