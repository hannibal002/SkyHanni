package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.features.dungeon.DungeonApi.DungeonClass
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.TextCompat.stripped
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

// TODO also fix up this all being coded very poorly and having the same patterns in multiple places
@SkyHanniModule
object DungeonFinderFeatures {
    private val config get() = SkyHanniMod.feature.dungeon.partyFinder

    //  Repo group and patterns
    private val patternGroup = RepoPattern.group("dungeon.finder.new")

    /**
     * REGEX-TEST: Note: 3m comp carry
     */
    private val pricePattern by patternGroup.pattern(
        "price",
        "(?i).*(?:[0-9]{2,3}K|[0-9]{1,3}M|[0-9]+\\.[0-9]M|[0-9] ?MIL).*",
    )

    /**
     * REGEX-TEST: Note: 3m comp carry
     * REGEX-TEST: Note: 250k comp carry
     */
    private val carryPattern by patternGroup.pattern(
        "carry",
        "(?i).*(?:CARRY|CARY|CARRIES|CARIES|COMP|TO CATA [0-9]{2}).*",
    )
    private val nonPugPattern by patternGroup.pattern(
        "nonpug",
        "(?i).*(?:PERM|VC|DISCORD).*",
    )

    /**
     * WRAPPED-REGEX-TEST: " 4sn_: Archer (29)"
     * WRAPPED-REGEX-TEST: " kaydo_odyak: Berserk (26)"
     * WRAPPED-REGEX-TEST: " ItsKind: Berserk (38)"
     * WRAPPED-REGEX-TEST: " sphxia: Tank (36)"
     * WRAPPED-REGEX-TEST: " Skept1x: Mage (35)"
     * WRAPPED-REGEX-TEST: " Mewlius: Archer (41)"
     */
    private val memberPattern by patternGroup.pattern(
        "member.colorless",
        " (?<playerName>.*): (?<className>.*?) \\(.*?(?<level>\\d+).*?\\)",
    )

    /**
     * REGEX-TEST: Requires a Class at Level 25!
     */
    private val ineligiblePattern by patternGroup.pattern(
        "ineligible",
        "Requires .*$|You don't meet the requirement!|Complete previous floor first!$",
    )

    /**
     * REGEX-TEST: Note: s+ clear first
     */
    private val notePattern by patternGroup.pattern(
        "note",
        "Note: (?<note>.*)",
    )

    /**
     * REGEX-TEST: The Catacombs
     * REGEX-TEST: MM The Catacombs
     */
    private val floorTypePattern by patternGroup.pattern(
        "floor.type",
        "The Catacombs.*|.*MM The Catacombs.*",
    )

    /**
     * REGEX-TEST: JohnRealNoob's Party
     */
    private val checkIfPartyPattern by patternGroup.pattern(
        "check.if.party",
        ".*'s Party",
    )
    private val partyFinderTitlePattern by patternGroup.pattern(
        "party.finder.title",
        "Party Finder",
    )
    private val catacombsGatePattern by patternGroup.pattern(
        "catacombs.gate",
        "Catacombs Gate",
    )
    private val selectFloorPattern by patternGroup.pattern(
        "select.floor",
        "Select Floor",
    )

    /**
     * REGEX-TEST: The Catacombs - Entrance
     */
    private val entranceFloorPattern by patternGroup.pattern(
        "entrance.colorless",
        ".*Entrance",
    )

    /**
     * REGEX-TEST: Floor VII
     * REGEX-TEST: Floor: Floor VII
     */
    private val floorPattern by patternGroup.pattern(
        "floor",
        "Floor:? .*",
    )
    private val anyFloorPattern by patternGroup.pattern(
        "floor.any",
        "Any",
    )

    /**
     * REGEX-TEST: Master Mode The Catacombs
     * REGEX-TEST: MM The Catacombs
     */
    private val masterModeFloorPattern by patternGroup.pattern(
        "floor.mastermode",
        "(?:MM|.*Master Mode) The Catacombs.*",
    )

    /**
     * REGEX-TEST: Dungeon: The Catacombs
     */
    private val dungeonFloorPattern by patternGroup.pattern(
        "floor.dungeon",
        "Dungeon: .*",
    )

    /**
     * REGEX-TEST: Floor VII
     */
    private val floorNumberPattern by patternGroup.pattern(
        "floor.number",
        ".* (?<floorNum>[IV\\d]+)",
    )

    /**
     * REGEX-TEST: Currently Selected: Mage
     */
    private val getDungeonClassPattern by patternGroup.pattern(
        "get.dungeon.class",
        "Currently Selected: (?<class>.*)",
    )
    private val detectDungeonClassPattern by patternGroup.pattern(
        "detect.dungeon.class",
        "View and select a dungeon class\\.",
    )

    // Variables used
    private var selectedClass: DungeonClass? = null
    private var floorStackSize = mapOf<Int, String>()
    private var highlightParty = mapOf<Int, LorenzColor>()
    private var toolTipMap = mapOf<Int, List<Component>>()
    private var inInventory = false

    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryOpen(event: InventoryOpenEvent) {
        val inventoryName = event.inventoryName
        val inventoryItems = event.inventoryItems
        floorStackSize = stackTip(inventoryName, inventoryItems)
        highlightParty = highlightingHandler(inventoryName, inventoryItems)
        toolTipMap = toolTipHandler(inventoryName, inventoryItems)
    }

    private fun stackTip(inventoryName: String, inventoryItems: Map<Int, SafeItemStack>): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        if (catacombsGatePattern.matches(inventoryName)) catacombsGateStackTip(inventoryItems, map)
        if (!config.floorAsStackSize) return map
        if (selectFloorPattern.matches(inventoryName)) selectFloorStackTip(inventoryItems, map)
        if (partyFinderTitlePattern.matches(inventoryName)) partyFinderStackTip(inventoryItems, map)
        return map
    }

    private fun selectFloorStackTip(inventoryItems: Map<Int, SafeItemStack>, map: MutableMap<Int, String>) {
        inInventory = true
        for ((slot, stack) in inventoryItems) {
            val name = stack.cleanName
            map[slot] = if (anyFloorPattern.matches(name)) {
                "A"
            } else if (entranceFloorPattern.matches(name)) {
                "E"
            } else if (floorPattern.matches(name)) {
                floorNumberPattern.matchMatcher(name) {
                    group("floorNum").romanToDecimalIfNecessary().toString()
                } ?: continue
            } else continue
        }
    }

    private fun partyFinderStackTip(inventoryItems: Map<Int, SafeItemStack>, map: MutableMap<Int, String>) {
        inInventory = true
        for ((slot, stack) in inventoryItems) {
            val name = stack.cleanName
            if (!checkIfPartyPattern.matches(name)) continue
            val lore = stack.getCleanLore()
            val floor = lore.find { floorPattern.matches(it) } ?: continue
            val dungeon = lore.find { dungeonFloorPattern.matches(it) } ?: continue
            val floorNum = floorNumberPattern.matchMatcher(floor) {
                group("floorNum").romanToDecimalIfNecessary()
            }
            map[slot] = getFloorName(floor, dungeon, floorNum)
        }
    }

    private fun catacombsGateStackTip(inventoryItems: Map<Int, SafeItemStack>, map: MutableMap<Int, String>) {
        val dungeonClassItemIndex = 45
        inInventory = true
        inventoryItems[dungeonClassItemIndex]?.getCleanLore()?.let {
            if (it.size > 3 && detectDungeonClassPattern.matches(it[0])) {
                getDungeonClassPattern.matchMatcher(it[2]) {
                    // This intentionally does not get cleared between lobbies
                    selectedClass = DungeonClass.getByClassName(group("class"))
                }
            }
        }

        if (!config.floorAsStackSize) return
        for ((slot, stack) in inventoryItems) {
            val name = stack.cleanName
            if (!floorTypePattern.matches(name)) continue
            val floorNum = floorNumberPattern.matchMatcher(name) {
                group("floorNum").romanToDecimalIfNecessary()
            } ?: continue
            map[slot] = getFloorName(name, name, floorNum)
        }
    }

    private fun getFloorName(floor: String, dungeon: String, floorNum: Int?): String = if (entranceFloorPattern.matches(floor)) {
        "E"
    } else if (masterModeFloorPattern.matches(dungeon)) {
        "M$floorNum"
    } else {
        "F$floorNum"
    }

    private fun highlightingHandler(inventoryName: String, inventoryItems: Map<Int, SafeItemStack>): Map<Int, LorenzColor> {
        val map = mutableMapOf<Int, LorenzColor>()
        if (!partyFinderTitlePattern.matches(inventoryName)) return map
        inInventory = true
        // TODO: Refactor this to not have so many continue statements
        @Suppress("LoopWithTooManyJumpStatements")
        for ((slot, stack) in inventoryItems) {
            val lore = stack.getCleanLore()
            if (!checkIfPartyPattern.matches(stack.cleanName)) continue
            if (config.markIneligibleGroups && ineligiblePattern.anyMatches(lore)) {
                map[slot] = LorenzColor.DARK_RED
                continue
            }

            if (config.markPaidCarries) {
                val note = lore.filter { notePattern.matches(it) }.joinToString(" ").uppercase()

                if (pricePattern.matches(note) && carryPattern.matches(note)) {
                    map[slot] = LorenzColor.RED
                    continue
                }
            }

            if (config.markNonPugs) {
                val note = lore.filter { notePattern.matches(it) }.joinToString(" ").uppercase()

                if (nonPugPattern.matches(note)) {
                    map[slot] = LorenzColor.LIGHT_PURPLE
                    continue
                }
            }

            val members = lore.filter { memberPattern.matches(it) }
            val memberLevels = members.map {
                memberPattern.matchMatcher(it) {
                    group("level").toInt()
                }
            }
            val memberClasses = members.map {
                memberPattern.matchMatcher(it) {
                    DungeonClass.getByClassName(group("className"))
                }
            }
            if (config.markBelowClassLevel != 0) {
                val hasLowLevelMembers = memberLevels.any { (it ?: Integer.MAX_VALUE) <= config.markBelowClassLevel }
                if (hasLowLevelMembers) {
                    map[slot] = LorenzColor.YELLOW
                    continue
                }
            }

            if (config.markMissingClass && memberClasses.none { it == selectedClass }) {
                map[slot] = LorenzColor.GREEN
            }
        }
        return map
    }

    private fun toolTipHandler(inventoryName: String, inventoryItems: Map<Int, SafeItemStack>): Map<Int, List<Component>> {
        val map = mutableMapOf<Int, List<Component>>()
        if (!partyFinderTitlePattern.matches(inventoryName)) return map
        inInventory = true
        for ((slot, stack) in inventoryItems) {
            val missingClasses = DungeonClass.entries.toMutableList()
            val cleanLore = stack.getCleanLore()
            val toolTip = stack.getLoreComponent().toMutableList()
            for ((index, line) in cleanLore.withIndex()) {
                memberPattern.matchMatcher(line) {
                    val playerName = group("playerName")
                    val className = group("className")
                    val level = group("level").toInt()
                    val levelComponent = DungeonApi.getLevelComponent(level)
                    if (config.coloredClassLevel) toolTip[index] = componentBuilder {
                        appendWithColor(" $playerName", ChatFormatting.AQUA)
                        appendWithColor(": ", ChatFormatting.WHITE)
                        appendWithColor("$className ", ChatFormatting.YELLOW)
                        append(levelComponent)
                    }
                    missingClasses.remove(DungeonClass.getByClassName(className))
                }
            }
            val name = cleanLore.firstOrNull()
            if (config.showMissingClasses && dungeonFloorPattern.matches(name)) {
                toolTip.add("")
                toolTip.add(
                    componentBuilder {
                        appendWithColor("Missing: ", ChatFormatting.RED)
                        missingClasses.forEachIndexed { index, dungeonClass ->
                            if (dungeonClass == selectedClass) {
                                appendWithColor(dungeonClass.displayName, ChatFormatting.GREEN)
                            } else {
                                appendWithColor(dungeonClass.displayName, ChatFormatting.GRAY)
                            }

                            if (index < missingClasses.size - 1) {
                                appendWithColor(", ", ChatFormatting.GRAY)
                            }
                        }
                    },
                )
            }
            if (toolTip.isNotEmpty()) {
                map[slot] = toolTip
            }
        }
        return map
    }

    @HandleEvent
    private fun onToolTip(event: ToolTipTextEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        event.slot ?: return

        val featureActive = config.let { it.coloredClassLevel || it.showMissingClasses }
        if (!featureActive) return

        val toolTip = toolTipMap[event.slot.index]
        if (toolTip.isNullOrEmpty()) return
        val oldToolTip = event.toolTip.toList()
        for ((index, line) in toolTip.withIndex()) {
            if (index >= event.toolTip.size - 1) {
                event.toolTip.add(line)
                continue
            }
            if (oldToolTip[index].stripped != line.stripped) {
                event.toolTip[index + 1] = line
            }
        }
    }

    @HandleEvent
    private fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!isEnabled()) return
        if (!config.floorAsStackSize) return
        val slot = event.slot
        if (slot.index != slot.containerSlot) return
        event.stackTip = (floorStackSize[slot.containerSlot]?.takeIf { it.isNotEmpty() } ?: return)
    }

    @HandleEvent
    private fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        event.container.slots.associateWith { highlightParty[it.index] }.forEach { (slot, color) ->
            color?.let { slot.highlight(it) }
        }
    }

    @HandleEvent
    private fun onInventoryClose() {
        inInventory = false
        floorStackSize = emptyMap()
        highlightParty = emptyMap()
        toolTipMap = emptyMap()
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "dungeon.partyFinderColoredClassLevel", "dungeon.partyFinder.coloredClassLevel")
    }

    // Since you can call Mort from anywhere, or use the command, this should not check for being in DUNGEON_HUB
    fun isEnabled() = SkyBlockUtils.inSkyBlock && selectedClass != null
}
