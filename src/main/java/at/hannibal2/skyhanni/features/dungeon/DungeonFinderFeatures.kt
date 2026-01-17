package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.createCommaSeparatedList
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

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
     * REGEX-TEST:  4sn_: Archer (29)
     * REGEX-TEST:  kaydo_odyak: Berserk (26)
     * REGEX-TEST:  ItsKind: §eBerserk§b (§e38§b)
     * REGEX-TEST:  sphxia: §eTank§b (§e36§b)
     * REGEX-TEST:  Skept1x: §eMage§b (§e35§b)
     * REGEX-TEST:  Mewlius: §eArcher§b (§e41§b)
     */
    private val memberPattern by patternGroup.pattern(
        "member",
        " (?<playerName>.*): (?<className>.*) \\((?<level>.*)\\)",
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
     * REGEX-TEST: §a§aThe Catacombs §8- §eEntrance
     */
    private val entranceFloorPattern by patternGroup.pattern(
        "entrance",
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

    //  Variables used
    private var selectedClass = ""
    private var floorStackSize = mapOf<Int, String>()
    private var highlightParty = mapOf<Int, LorenzColor>()
    private var toolTipMap = mapOf<Int, List<Component>>()
    private var inInventory = false

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return

        floorStackSize = stackTip(event)
        highlightParty = highlightingHandler(event)
        toolTipMap = toolTipHandler(event)
    }

    private fun stackTip(event: InventoryOpenEvent): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        val inventoryName = event.inventoryName
        if (catacombsGatePattern.matches(inventoryName)) catacombsGateStackTip(event.inventoryItems, map)
        if (!config.floorAsStackSize) return map
        if (selectFloorPattern.matches(inventoryName)) selectFloorStackTip(event.inventoryItems, map)
        if (partyFinderTitlePattern.matches(inventoryName)) partyFinderStackTip(event.inventoryItems, map)
        return map
    }

    private fun selectFloorStackTip(inventoryItems: Map<Int, ItemStack>, map: MutableMap<Int, String>) {
        inInventory = true
        for ((slot, stack) in inventoryItems) {
            val name = stack.hoverName.string.removeColor()
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

    private fun partyFinderStackTip(inventoryItems: Map<Int, ItemStack>, map: MutableMap<Int, String>) {
        inInventory = true
        for ((slot, stack) in inventoryItems) {
            val name = stack.cleanName()
            if (!checkIfPartyPattern.matches(name)) continue
            val lore = stack.getLoreComponent()
            val floor = lore.find { floorPattern.matches(it.string) } ?: continue
            val dungeon = lore.find { dungeonFloorPattern.matches(it.string) } ?: continue
            val floorNum = floorNumberPattern.matchMatcher(floor) {
                group("floorNum").romanToDecimalIfNecessary()
            }
            map[slot] = getFloorName(floor.string, dungeon.string, floorNum)
        }
    }

    private fun catacombsGateStackTip(inventoryItems: Map<Int, ItemStack>, map: MutableMap<Int, String>) {
        val dungeonClassItemIndex = 45
        inInventory = true
        inventoryItems[dungeonClassItemIndex]?.getLoreComponent()?.let {
            if (it.size > 3 && detectDungeonClassPattern.matches(it[0])) {
                getDungeonClassPattern.matchMatcher(it[2].string) {
                    selectedClass = group("class")
                }
            }
        }

        if (!config.floorAsStackSize) return
        for ((slot, stack) in inventoryItems) {
            val name = stack.hoverName.string.removeColor()
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

    private fun highlightingHandler(event: InventoryOpenEvent): Map<Int, LorenzColor> {
        val map = mutableMapOf<Int, LorenzColor>()
        if (!partyFinderTitlePattern.matches(event.inventoryName)) return map
        inInventory = true
        // TODO: Refactor this to not have so many continue statements
        @Suppress("LoopWithTooManyJumpStatements")
        for ((slot, stack) in event.inventoryItems) {
            val lore = stack.getLoreComponent()
            if (!checkIfPartyPattern.matches(stack.cleanName())) continue
            if (config.markIneligibleGroups && ineligiblePattern.anyMatches(lore.map { it.string })) {
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
                    group("className")
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

    private fun toolTipHandler(event: InventoryOpenEvent): Map<Int, List<Component>> {
        val map = mutableMapOf<Int, List<Component>>()
        val inventoryName = event.inventoryName
        if (!partyFinderTitlePattern.matches(inventoryName)) return map
        inInventory = true
        for ((slot, stack) in event.inventoryItems) {
            // TODO use enum
            val classNames = mutableListOf("Healer", "Mage", "Berserk", "Archer", "Tank")
            val toolTip = stack.getLoreComponent().toMutableList()
            for ((index, line) in stack.getLoreComponent().withIndex()) {
                memberPattern.matchMatcher(line) {
                    val playerName = group("playerName")
                    val className = group("className")
                    val level = group("level").toInt()
                    val color = DungeonApi.getColor(level)
                    if (config.coloredClassLevel) toolTip[index] = " §b$playerName§f: §e$className $color$level".asComponent()
                    classNames.remove(className)
                }
            }
            val name = stack.getLoreComponent().firstOrNull()
            if (config.showMissingClasses && dungeonFloorPattern.matches(name)) {
                if (classNames.contains(selectedClass)) {
                    classNames[classNames.indexOf(selectedClass)] = "§a$selectedClass§7"
                }
                toolTip.add("")
                toolTip.add("§cMissing: §7" + classNames.createCommaSeparatedList())
            }
            if (toolTip.isNotEmpty()) {
                map[slot] = toolTip
            }
        }
        return map
    }

    @HandleEvent
    fun onToolTip(event: ToolTipTextEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        event.slot ?: return

        val featureActive = config.let { it.coloredClassLevel || it.showMissingClasses }
        if (!featureActive) return

        val toolTip = toolTipMap[event.slot.index]
        if (toolTip.isNullOrEmpty()) return
        val oldToolTip = event.toolTip
        for ((index, line) in toolTip.withIndex()) {
            if (index >= event.toolTip.size - 1) {
                event.toolTip.add(line)
                continue
            }
            if (oldToolTip[index] != line) event.toolTip[index + 1] = line
        }
    }

    @HandleEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!isEnabled()) return
        if (!config.floorAsStackSize) return
        val slot = event.slot
        if (slot.index != slot.containerSlot) return
        event.stackTip = (floorStackSize[slot.containerSlot]?.takeIf { it.isNotEmpty() } ?: return)
    }

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        event.container.slots.associateWith { highlightParty[it.index] }.forEach { (slot, color) ->
            color?.let { slot.highlight(it) }
        }
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        floorStackSize = emptyMap()
        highlightParty = emptyMap()
        toolTipMap = emptyMap()
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "dungeon.partyFinderColoredClassLevel", "dungeon.partyFinder.coloredClassLevel")
    }

    // todo use graph area
    fun isEnabled() = SkyBlockUtils.inSkyBlock && SkyBlockUtils.scoreboardArea == "Dungeon Hub"
}
