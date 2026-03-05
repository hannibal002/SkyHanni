package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.TabListUpdateComponentEvent
import at.hannibal2.skyhanni.events.TablistFooterUpdateComponentEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
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
    private val dataColumns = mutableListOf<TabColumn>()

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
        "effects.active",
        "Active Effects",
    )

    /**
     * REGEX-TEST: You have 1 active effect. Use "/effects" to see it!
     */
    private val effectCountPattern by patternGroup.pattern(
        "effects.count",
        "You have (?<effectCount>[0-9]+) active effect",
    )

    /**
     * REGEX-TEST: Cookie Buff
     */
    private val cookiePattern by patternGroup.pattern(
        "cookie",
        "Cookie Buff",
    )

    /**
     * REGEX-TEST: Dungeon Buffs
     */
    private val dungeonBuffPattern by patternGroup.pattern(
        "dungeonbuff",
        "Dungeon Buffs",
    )

    // TODO: Regex tests
    private val upgradesPattern by patternGroup.pattern(
        "upgrades",
        "(?<firstPart>[A-Za-z ]+)(?<secondPart> [\\w ]+)"
    )

    // TODO: Regex tests
    private val winterPowerUpsPattern by patternGroup.pattern(
        "winterpowerups",
        "Active Power Ups",
    )

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.enabled) {
            rebuildRenderColumns()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabListUpdate(event: TabListUpdateComponentEvent) {
        this.lastTabComponents = event.tabList
        rebuildRenderColumns()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabListFooterUpdate(event: TablistFooterUpdateComponentEvent) {
        this.lastFooterComponent = event.footer
        rebuildRenderColumns()
    }

    private fun rebuildRenderColumns() {
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
        val columns = mutableListOf<TabColumn>()
        val fullTabComponents = AdvancedPlayerList.newSorting(components)

        for (entry in fullTabComponents.indices step 20) {
            val title = fullTabComponents[entry].string
            var column = getColumnFromName(columns, title)

            if (column == null) {
                column = TabColumn(title)
                columns.add(column)
            }

            for (columnEntry in (entry + 1) until fullTabComponents.size.coerceAtMost(entry + 20)) {
                column.addComponent(fullTabComponents[columnEntry])
            }
        }
        return columns
    }

    private fun parseFooterAsColumn(component: Component): TabColumn? {
        val lines = TextHelper.split(component, "\n") ?: listOf(component)

        val godPotTimer = lines.firstNotNullOfOrNull {
            godPotPattern.matchMatcher(it.string) { group("timer") }
        }
        val effectCount = lines.firstNotNullOfOrNull {
            effectCountPattern.matchMatcher(it.string) { group("effectCount") }
        }

        return TabColumn("§2§lOther").apply {
            for (lineComponent in lines) {
                val lineStr = lineComponent.string
                if (lineStr.contains(hypixelAdvertisingString)) continue

                // These lines were consumed into the active effects header — skip them
                if (godPotTimer != null && godPotPattern.matches(lineStr)) continue
                if (effectCountPattern.matches(lineStr)) continue

                when {
                    activeEffectPattern.matches(lineStr) -> {
                        when {
                            godPotTimer != null -> {
                                addComponent(Component.literal("Active Effects:"))
                                addComponent(Component.literal(" God Potion: $godPotTimer"))
                            }
                            effectCount != null -> addComponent(Component.literal("Active Effects: $effectCount"))
                            else -> addComponent(Component.literal("Active Effects: 0"))
                        }
                    }
                    
                    cookiePattern.matches(lineStr) -> {
                        // Fallthrough to not active check
                        addComponent(Component.literal("Cookie Buff"))
                    }
                    lineStr.startsWith("Not active!") &&
                        components.lastOrNull()?.string == "Cookie Buff" -> {
                        addComponent(Component.literal(" Not Active"))
                    }

                    dungeonBuffPattern.matches(lineStr) -> {
                        addComponent(Component.literal("Dungeon Buffs"))
                    }
                    lineStr.startsWith("No Buffs active.") &&
                        components.lastOrNull()?.string == "Dungeon Buffs" -> {
                        addComponent(Component.literal(" None Found"))
                    }

                    winterPowerUpsPattern.matches(lineStr) -> {
                        addComponent(Component.literal("Active Power Ups"))
                    }
                    lineStr.startsWith("No Power Ups active.") &&
                        components.lastOrNull()?.string == "Active Power Ups" -> {
                        addComponent(Component.literal(" None"))
                    }
                    
                    upgradesPattern.matches(lineStr) -> {
                        upgradesPattern.matchMatcher(lineStr) {
                            var firstPart = group("firstPart")
                            if (!lineComponent.style.isBold) firstPart = " $firstPart"
                            addComponent(Component.literal(firstPart))
                            addComponent(Component.literal(group("secondPart")))
                        }
                    }

                    else -> {
                        var newLine = lineStr
                        if (!lineComponent.style.isBold) newLine = " $newLine"
                        addComponent(Component.literal(newLine))
                    }
                }
            }
        }.takeIf { it.components.isNotEmpty() }
    }

    private fun getColumnFromName(columns: List<TabColumn>, name: String): TabColumn? {
        for (tabColumn in columns) {
            if (name == tabColumn.columnTitle) {
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
        var lastTitle: String? = null

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
            val needsTitle = lastTitle != section.columnValue.columnTitle
            val sectionSize = section.size() + if (needsTitle) 1 else 0
            val isLarge = sectionSize >= TabListRenderer.MAX_LINES / 2

            newColumnOrSpacer(required = isLarge && currentColumn.size() >= TabListRenderer.MAX_LINES)

            if (needsTitle) {
                lastTitle = section.columnValue.columnTitle
                currentColumn.addLine(AdvancedPlayerList.createTabLine(Component.literal(lastTitle), TabStringType.TITLE))
            }

            for (line in section.components) addLine(line)
        }
    }
}
