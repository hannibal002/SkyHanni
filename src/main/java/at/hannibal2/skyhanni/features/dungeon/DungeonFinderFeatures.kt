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
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration

class DungeonFinderFeatures {
    private val config get() = SkyHanniMod.feature.dungeon.partyFinder

//  Repo group and patterns
    private val repoGroup = RepoPattern.group("dungeon.finder.features")
    private val pricePattern by repoGroup.pattern("price", "([0-9]{2,3}K|[0-9]{1,3}M|[0-9]+\\.[0-9]M|[0-9] ?MIL)")
    private val carryPattern by repoGroup.pattern("carry", "(CARRY|CARY|CARRIES|CARIES|COMP|TO CATA [0-9]{2})")
    private val nonPugPattern by repoGroup.pattern("nonpug", "(PERM|VC|DISCORD)")
    private val memberPattern by repoGroup.pattern("member", ".*§.\\w+§f: §e(\\w+)§b \\(§e(\\d+)§b\\)")
    private val ineligiblePattern by repoGroup.pattern("ineligible", "^§c(Requires .*$|You don't meet the requirement!|Complete previous floor first!$)")
    private val classLevelPattern by repoGroup.pattern("classlevel", " §.(?<playerName>.*)§f: §e(?<className>.*)§b \\(§e(?<level>.*)§b\\)")
    private val notePattern by repoGroup.pattern("note", "^(§7§7Note: |§f[^§])")
    private val floorTypePattern by repoGroup.pattern("floortype", "(The Catacombs).*|.*(MM Catacombs).*")
    private val checkIfPartyPattern by repoGroup.pattern("checkifparty", ".*('s Party)")
    private val partyFinderTitlePattern by repoGroup.pattern("pfindertitle", "(Party Finder)")
    private val catacombsGatePattern by repoGroup.pattern("catagate", "(Catacombs Gate)")
    private val selectFloorPattern by repoGroup.pattern("selectfloor", "(Select Floor)")
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

        DelayedRun.runDelayed(Duration.parse("10ms")) {
            floorStackSize.clear()
            highlightParty.clear()
//
//        Code for Stack Tip
//
            if(catacombsGatePattern.matches(inventoryName)) {
                val lore = event.inventoryItems[45]?.getLore()
                inInventory = true
                if (!lore.isNullOrEmpty() && lore[0] == "§7View and select a dungeon class.") {
                    selectedClass = lore[2].split(" ").last().removeColor()
                }

                for((slot, stack) in event.inventoryItems) {
                    val name = stack.displayName.removeColor()
                    if(floorTypePattern.matches(name)) {
                        val floorNum = name.split(" ").last().romanToDecimalIfNecessary().toString()
                        if(name.contains("Entrance")) {
                            floorStackSize[slot] = "E"
                        } else if(name.contains("MM ")) {
                            floorStackSize[slot] = "M$floorNum"
                        } else {
                            floorStackSize[slot] = floorNum
                        }
                    }
                }
            }

            if(config.floorAsStackSize) {
                if(selectFloorPattern.matches(inventoryName)) {
                    inInventory = true
                    for((slot, stack) in event.inventoryItems) {
                        if(stack.displayName == "Any") {
                            floorStackSize[slot] = "A"
                        } else if(stack.displayName == "Entrance") {
                            floorStackSize[slot] = "E"
                        } else if(stack.displayName.startsWith("Floor ")) {
                            floorStackSize[slot] = stack.displayName.split(' ').last().romanToDecimalIfNecessary().toString()
                        }
                    }
                }

                if(partyFinderTitlePattern.matches(inventoryName)) {
                    inInventory = true
                    for((slot, stack) in event.inventoryItems) {
                        val name = stack.displayName.removeColor()
                        if(checkIfPartyPattern.matches(name)) {
                            val lore = stack.getLore()
                            val floor = lore.find { it.startsWith("§7Floor: ") } ?: return@runDelayed
                            val dungeon = lore.find { it.startsWith("§7Dungeon: ") } ?: return@runDelayed
                            val floorNum = floor.split(' ').last().romanToDecimalIfNecessary().toString()
                            if (floor.contains("Entrance")) {
                                floorStackSize[slot] = "E"
                            } else if (dungeon.contains("Master Mode")) {
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
            if(partyFinderTitlePattern.matches(inventoryName)) {
                inInventory = true
                for((slot, stack) in event.inventoryItems) {
                    val lore = stack.getLore()
                    if(!checkIfPartyPattern.matches(stack.displayName)) continue
                    if (config.markIneligibleGroups && lore.any { ineligiblePattern.matches(it) }) {
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
                            group(2).toInt()
                        } }
                    val memberClasses = members.map {
                        memberPattern.matchMatcher(it) {
                            group(1)
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

            if(partyFinderTitlePattern.matches(inventoryName)) {
                inInventory = true
                for((slot, stack) in event.inventoryItems) {
                    val classNames = mutableListOf("Healer", "Mage", "Berserk", "Archer", "Tank")
                    val toolTip = stack.getLore().toMutableList()
                    for ((index, line) in stack.getLore().withIndex()) {
                        classLevelPattern.matchMatcher(line) {
                            val playerName = group(1)
                            val className = group(2)
                            val level = group(3).toInt()
                            val color = getColor(level)
                            if (config.coloredClassLevel) toolTip[index] = " §b$playerName§f: §e$className $color$level"
                            classNames.remove(className)
                        }
                    }
                    if (config.showMissingClasses && stack.getLore().firstOrNull()?.removeColor()?.startsWith("Dungeon:") == true) {
                        if (classNames.contains(selectedClass)) classNames[classNames.indexOf(selectedClass)] = "§a${selectedClass}§7"
                        toolTip.add("")
                        toolTip.add("§cMissing: §7" + StringUtils.createCommaSeparatedList(classNames))
                    }
                    if(toolTip.isNotEmpty()) toolTipMap[slot] = toolTip
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
        toolTip.withIndex().forEach { (index, line) ->
            if(index >= event.toolTip.size) event.toolTip.add(line)
                else event.toolTip[index] = line
        }
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if(!isEnabled()) return
        if(!config.floorAsStackSize) return
        if(floorStackSize[event.slot.slotIndex].isNullOrEmpty()) return
        event.stackTip = floorStackSize[event.slot.slotIndex].toString()
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

