package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.anyMatches
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

class DungeonFinderFeatures {
    private val config get() = SkyHanniMod.feature.dungeon.partyFinder

    //  Repo group and patterns
    private val patternGroup = RepoPattern.group("dungeon.finder.features")
    private val pricePattern by patternGroup.pattern(
        "price",
        "([0-9]{2,3}K|[0-9]{1,3}M|[0-9]+\\.[0-9]M|[0-9] ?MIL)"
    )
    private val carryPattern by patternGroup.pattern(
        "carry",
        "(CARRY|CARY|CARRIES|CARIES|COMP|TO CATA [0-9]{2})"
    )
    private val nonPugPattern by patternGroup.pattern(
        "nonpug",
        "(PERM|VC|DISCORD)"
    )
    private val memberPattern by patternGroup.pattern(
        "member",
        ".*§.(?<playerName>.*)§f: §e(?<className>.*)§b \\(§e(?<level>.*)§b\\)"
    )
    private val ineligiblePattern by patternGroup.pattern(
        "ineligible",
        "§c(Requires .*$|You don't meet the requirement!|Complete previous floor first!$)"
    )
    private val classLevelPattern by patternGroup.pattern(
        "class.level",
        " §.(?<playerName>.*)§f: §e(?<className>.*)§b \\(§e(?<level>.*)§b\\)"
    )
    private val notePattern by patternGroup.pattern(
        "note",
        "(§7§7Note: |§f[^§])"
    )
    private val floorTypePattern by patternGroup.pattern(
        "floor.type",
        "(The Catacombs).*|.*(MM Catacombs).*"
    )
    private val checkIfPartyPattern by patternGroup.pattern(
        "check.if.party",
        ".*('s Party)"
    )
    private val partyFinderTitlePattern by patternGroup.pattern(
        "party.finder.title",
        "(Party Finder)"
    )
    private val catacombsGatePattern by patternGroup.pattern(
        "catacombs.gate",
        "(Catacombs Gate)"
    )
    private val selectFloorPattern by patternGroup.pattern(
        "select.floor",
        "(Select Floor)"
    )
    private val entranceFloorPattern by patternGroup.pattern(
        "entrance",
        "(.*Entrance)"
    )
    private val floorPattern by patternGroup.pattern(
        "floor",
        "(Floor .*)"
    )
    private val anyFloorPattern by patternGroup.pattern(
        "floor.any",
        "(Any)"
    )
    private val masterModeFloorPattern by patternGroup.pattern(
        "floor.mastermode",
        "(MM )|(.*Master Mode Catacombs)"
    )
    private val dungeonFloorPattern by patternGroup.pattern(
        "floor.dungeon",
        "(Dungeon: .*)"
    )
    private val floorFloorPattern by patternGroup.pattern(
        "floor.pattern",
        "(Floor: .*)"
    )
    private val floorNumberPattern by patternGroup.pattern(
        "floor.number",
        ".* (?<floorNum>[IV\\d]+)"
    )
    private val getDungeonClassPattern by patternGroup.pattern(
        "get.dungeon.class",
        ".* (?<class>.*)"
    )
    private val detectDungeonClassPattern by patternGroup.pattern(
        "detect.dungeon.class",
        "(View and select a dungeon class.)"
    )

    //  Variables used
    private var selectedClass = ""
    private var floorStackSize = mutableMapOf<Int, String>()
    private var highlightParty = mutableMapOf<Int, LorenzColor>()
    private var toolTipMap = mutableMapOf<Int, MutableList<String>>()
    private var inInventory = false

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return

        DelayedRun.runDelayed(10.0.milliseconds) {
            floorStackSize.clear()
            highlightParty.clear()
            toolTipMap.clear()

            stackTip(event)

            highlightingHandler(event)

            toolTipHandler(event)
        }

    }

    private fun stackTip(event: InventoryFullyOpenedEvent) {
        val inventoryName = event.inventoryName
        if (catacombsGatePattern.matches(inventoryName)) catacombsGateStackTip(event)
        if (!config.floorAsStackSize) return
        if (selectFloorPattern.matches(inventoryName)) selectFloorStackTip(event)
        if (partyFinderTitlePattern.matches(inventoryName)) partyFinderStackTip(event)
    }

    private fun selectFloorStackTip(event: InventoryFullyOpenedEvent) {
        inInventory = true
        for ((slot, stack) in event.inventoryItems) {
            val name = stack.displayName.removeColor()
            if (anyFloorPattern.matches(name)) {
                floorStackSize[slot] = "A"
            } else if (entranceFloorPattern.matches(name)) {
                floorStackSize[slot] = "E"
            } else if (floorPattern.matches(name)) {
                floorStackSize[slot] =
                    floorNumberPattern.matchMatcher(name) { group("floorNum").romanToDecimalIfNecessary().toString() } ?: continue
            }
        }
    }

    private fun partyFinderStackTip(event: InventoryFullyOpenedEvent) {
        inInventory = true
        for ((slot, stack) in event.inventoryItems) {
            val name = stack.displayName.removeColor()
            if (checkIfPartyPattern.matches(name)) {
                val lore = stack.getLore()
                val floor = lore.find { floorFloorPattern.matches(it.removeColor()) }
                val dungeon =
                    lore.find { dungeonFloorPattern.matches(it.removeColor()) }
                if (floor == null || dungeon == null) continue
                val floorNum =
                    floorNumberPattern.matchMatcher(floor) { group("floorNum").romanToDecimalIfNecessary().toString() }
                if (entranceFloorPattern.matches(floor)) {
                    floorStackSize[slot] = "E"
                } else if (masterModeFloorPattern.matches(dungeon)) {
                    floorStackSize[slot] = "M$floorNum"
                } else {
                    floorStackSize[slot] = "F$floorNum"
                }
            }
        }

    }

    private fun catacombsGateStackTip(event: InventoryFullyOpenedEvent) {
        val dungeonClassItemIndex = 45
        val lore = event.inventoryItems[dungeonClassItemIndex]?.getLore()
        inInventory = true
        if (lore != null) {
            if (lore.size > 3 && detectDungeonClassPattern.matches(lore[0])) {
                selectedClass = getDungeonClassPattern.matchMatcher(lore[2].removeColor()) {
                    group("class")
                }.toString()
            }
        }

        if (config.floorAsStackSize) {
            for ((slot, stack) in event.inventoryItems) {
                val name = stack.displayName.removeColor()
                if (floorTypePattern.matches(name)) {
                    val floorNum =
                        floorNumberPattern.matchMatcher(name) {
                            group("floorNum").romanToDecimalIfNecessary().toString()
                        } ?: continue
                    if (entranceFloorPattern.matches(name)) {
                        floorStackSize[slot] = "E"
                    } else if (masterModeFloorPattern.matches(name)) {
                        floorStackSize[slot] = "M$floorNum"
                    } else {
                        floorStackSize[slot] = floorNum
                    }
                }
            }
        }

    }

    private fun highlightingHandler(event: InventoryFullyOpenedEvent) {
        val inventoryName = event.inventoryName
        if (partyFinderTitlePattern.matches(inventoryName)) {
            inInventory = true
            for ((slot, stack) in event.inventoryItems) {
                val lore = stack.getLore()
                if (!checkIfPartyPattern.matches(stack.displayName)) continue
                if (config.markIneligibleGroups && ineligiblePattern.anyMatches(lore)) {
                    highlightParty[slot] = LorenzColor.DARK_RED
                    continue
                }

                if (config.markPaidCarries) {
                    val note = lore.filter { notePattern.matches(it) }.joinToString(" ").uppercase()

                    if (pricePattern.matches(note) && carryPattern.matches(note)) {
                        highlightParty[slot] = LorenzColor.RED
                        continue
                    }
                }

                if (config.markNonPugs) {
                    val note = lore.filter { notePattern.matches(it) }.joinToString(" ").uppercase()

                    if (nonPugPattern.matches(note)) {
                        highlightParty[slot] = LorenzColor.LIGHT_PURPLE
                        continue
                    }
                }

                val members = lore.filter {
                    memberPattern.matches(it)
                }
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
                if (memberLevels.any { (it ?: Integer.MAX_VALUE) <= config.markBelowClassLevel }) {
                    highlightParty[slot] = LorenzColor.YELLOW
                    continue
                }

                if (config.markMissingClass && memberClasses.none { it == selectedClass }) {
                    highlightParty[slot] = LorenzColor.GREEN
                }
            }
        }
    }

    private fun toolTipHandler(event: InventoryFullyOpenedEvent) {
        val inventoryName = event.inventoryName
        if (partyFinderTitlePattern.matches(inventoryName)) {
            inInventory = true
            for ((slot, stack) in event.inventoryItems) {
                val classNames = mutableListOf("Healer", "Mage", "Berserk", "Archer", "Tank")
                val toolTip = stack.getLore().toMutableList()
                for ((index, line) in stack.getLore().withIndex()) {
                    classLevelPattern.matchMatcher(line) {
                        val playerName = group("playerName")
                        val className = group("className")
                        val level = group("level").toInt()
                        val color = getColor(level)
                        if (config.coloredClassLevel) toolTip[index] = " §b$playerName§f: §e$className $color$level"
                        classNames.remove(className)
                    }
                }
                if (config.showMissingClasses && dungeonFloorPattern.matches(
                        stack.getLore().firstOrNull()?.removeColor()
                    )
                ) {
                    if (classNames.contains(selectedClass)) classNames[classNames.indexOf(selectedClass)] =
                        "§a${selectedClass}§7"
                    toolTip.add("")
                    toolTip.add("§cMissing: §7" + StringUtils.createCommaSeparatedList(classNames))
                }
                if (toolTip.isNotEmpty()) toolTipMap[slot] = toolTip
            }
        }
    }

    @SubscribeEvent
    fun onToolTipRender(event: LorenzToolTipEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        val toolTip = toolTipMap[event.slot.slotIndex]
        if (toolTip.isNullOrEmpty()) return
        // TODO @Thunderblade73 fix that to "event.toolTip = toolTip"
        val oldToolTip = event.toolTip
        for ((index, line) in toolTip.withIndex()) {
            if (index >= event.toolTip.size - 1) {
                event.toolTip.add(line)
                continue
            }
            if (oldToolTip[index] != line) event.toolTip[index + 1] = line
        }
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!isEnabled()) return
        if (!config.floorAsStackSize) return
        if (floorStackSize[event.slot.slotIndex].isNullOrEmpty()) return
        event.stackTip = floorStackSize[event.slot.slotIndex] ?: ""
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        event.gui.inventorySlots.inventorySlots
            .associateWith { highlightParty[it.slotNumber] }
            .forEach { (slot, color) ->
                color?.let { slot.highlight(it) }
            }

    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        floorStackSize.clear()
        highlightParty.clear()
        toolTipMap.clear()
    }

    companion object {
        fun getColor(level: Int): String {
            return when {
                level >= 30 -> "§a"
                level >= 25 -> "§b"
                level >= 20 -> "§e"
                level >= 15 -> "§6"
                level >= 10 -> "§c"
                else -> "§4"
            }
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockArea == "Dungeon Hub"
}
