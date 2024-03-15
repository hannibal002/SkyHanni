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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class DungeonFinderFeatures {
    private val config get() = SkyHanniMod.feature.dungeon.partyFinder

    //  Repo group and patterns
    private val repoGroup = RepoPattern.group("dungeon.finder.features")
    private val pricePattern by repoGroup.pattern("price", "([0-9]{2,3}K|[0-9]{1,3}M|[0-9]+\\.[0-9]M|[0-9] ?MIL)")
    private val carryPattern by repoGroup.pattern("carry", "(CARRY|CARY|CARRIES|CARIES|COMP|TO CATA [0-9]{2})")
    private val nonPugPattern by repoGroup.pattern("nonpug", "(PERM|VC|DISCORD)")
    private val memberPattern by repoGroup.pattern("member", ".*§.(?<playerName>.*)§f: §e(?<className>.*)§b \\(§e(?<level>.*)§b\\)")
    private val ineligiblePattern by repoGroup.pattern(
        "ineligible",
        "^§c(Requires .*$|You don't meet the requirement!|Complete previous floor first!$)"
    )
    private val classLevelPattern by repoGroup.pattern(
        "classlevel",
        " §.(?<playerName>.*)§f: §e(?<className>.*)§b \\(§e(?<level>.*)§b\\)"
    )
    private val notePattern by repoGroup.pattern("note", "^(§7§7Note: |§f[^§])")
    private val floorTypePattern by repoGroup.pattern("floortype", "(The Catacombs).*|.*(MM Catacombs).*")
    private val checkIfPartyPattern by repoGroup.pattern("checkifparty", ".*('s Party)")
    private val partyFinderTitlePattern by repoGroup.pattern("pfindertitle", "(Party Finder)")
    private val catacombsGatePattern by repoGroup.pattern("catagate", "(Catacombs Gate)")
    private val selectFloorPattern by repoGroup.pattern("selectfloor", "(Select Floor)")
    private val entranceFloorPattern by repoGroup.pattern("entrance", "(.*Entrance)")
    private val floorPattern by repoGroup.pattern("floor", "(Floor .*)")
    private val anyFloorPattern by repoGroup.pattern("anyfloor", "(Any)")
    private val masterModeFloorPattern by repoGroup.pattern("mmfloor", "(MM )|(.*Master Mode Catacombs)")
    private val dungeonFloorPattern by repoGroup.pattern("dungeonfloor", "(Dungeon: .*)")
    private val floorFloorPattern by repoGroup.pattern("floorpattern", "(Floor: .*)")

    //  Variables used
    private var selectedClass = ""
    private var floorStackSize = mutableMapOf<Int, String>()
    private var highlightParty = mutableMapOf<Int, LorenzColor>()
    private var toolTipMap = mutableMapOf<Int, MutableList<String>>()
    private var inInventory = false

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        val inventoryName = event.inventoryName

        DelayedRun.runDelayed(10.0.milliseconds) {
            floorStackSize.clear()
            highlightParty.clear()
//
//        Code for Stack Tip
//
            if (catacombsGatePattern.matches(inventoryName)) {
                val lore = event.inventoryItems[45]?.getLore()
                inInventory = true
                if (lore != null) {
                    if (lore.size > 3 && lore[0] == "§7View and select a dungeon class.") {
                        selectedClass = lore[2].split(" ").last().removeColor()
                    }
                }

                if(config.floorAsStackSize) {
                    for ((slot, stack) in event.inventoryItems) {
                        val name = stack.displayName.removeColor()
                        if (floorTypePattern.matches(name)) {
                            val floorNum = name.split(" ").last().romanToDecimalIfNecessary().toString()
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

            if (config.floorAsStackSize) {
                if (selectFloorPattern.matches(inventoryName)) {
                    inInventory = true
                    for ((slot, stack) in event.inventoryItems) {
                        val name = stack.displayName.removeColor()
                        if (anyFloorPattern.matches(name)) {
                            floorStackSize[slot] = "A"
                        } else if (entranceFloorPattern.matches(name)) {
                            floorStackSize[slot] = "E"
                        } else if (floorPattern.matches(name)) {
                            floorStackSize[slot] =
                                name.split(' ').last().romanToDecimalIfNecessary().toString()
                        }
                    }
                }

                if (partyFinderTitlePattern.matches(inventoryName)) {
                    inInventory = true
                    for ((slot, stack) in event.inventoryItems) {
                        val name = stack.displayName.removeColor()
                        if (checkIfPartyPattern.matches(name)) {
                            val lore = stack.getLore()
                            val floor = lore.find { floorFloorPattern.matches(it.removeColor()) }
                            val dungeon =
                                lore.find { dungeonFloorPattern.matches(it.removeColor()) }
                            if (floor == null || dungeon == null) continue
                            val floorNum = floor.split(' ').last().romanToDecimalIfNecessary().toString()
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
            }
//
//        Code for Highlighting
//
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
                    if (memberLevels.any { it!! <= config.markBelowClassLevel }) {
                        highlightParty[slot] = LorenzColor.YELLOW
                        continue
                    }

                    if (config.markMissingClass && memberClasses.none { it == selectedClass }) {
                        highlightParty[slot] = LorenzColor.GREEN
                    }
                }
            }
//
//        Code for ToolTip
//

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


    }

    @SubscribeEvent
    fun onToolTipRender(event: LorenzToolTipEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        val toolTip = toolTipMap[event.slot.slotIndex]
        if (toolTip.isNullOrEmpty()) return
        val oldToolTip = event.toolTip
        for((index, line) in toolTip.withIndex()) {
            if (index >= event.toolTip.size-1) {
                event.toolTip.add(line)
                continue
            }
            if(oldToolTip[index] != line) event.toolTip[index + 1] = line
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
            .filter { highlightParty.containsKey(it.slotNumber) }
            .forEach { it highlight highlightParty[it.slotNumber]!! }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        floorStackSize.clear()
        highlightParty.clear()
        toolTipMap.clear()
    }

    @SubscribeEvent
    fun onInventoryClose(event: GuiContainerEvent.CloseWindowEvent) {
        inInventory = false
        floorStackSize.clear()
        highlightParty.clear()
        toolTipMap.clear()
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockArea == "Dungeon Hub"
}

fun getColor(level: Int): String {
    if (level >= 50) return "§c§l"
    if (level >= 45) return "§c"
    if (level >= 40) return "§d"
    if (level >= 35) return "§6"
    if (level >= 30) return "§5"
    if (level >= 25) return "§9"
    if (level >= 20) return "§a"
    if (level >= 10) return "§f"
    return "§7"
}

