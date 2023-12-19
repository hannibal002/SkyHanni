package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonFinderFeatures {
    private val config get() = SkyHanniMod.feature.dungeon.partyFinder

    private val pricePattern = "([0-9]{2,3}K|[0-9]{1,3}M|[0-9]+\\.[0-9]M|[0-9] ?mil)".toRegex(RegexOption.IGNORE_CASE)
    private val carryPattern = "(carry|cary|carries|caries|comp|to cata [0-9]{2})".toRegex(RegexOption.IGNORE_CASE)
    private val nonPugPattern = "(perm|vc|discord)".toRegex(RegexOption.IGNORE_CASE)
    private val memberPattern = "^ §.*?§.: §.([A-Z]+)§. \\(§.([0-9]+)§.\\)".toRegex(RegexOption.IGNORE_CASE)
    private val ineligiblePattern =
        "^§c(Requires .*$|You don't meet the requirement!|Complete previous floor first!$)".toRegex()
    private val classLevelPattern = " §.(?<playerName>.*)§f: §e(?<className>.*)§b \\(§e(?<level>.*)§b\\)".toPattern()
    private val notePattern = "^(§7§7Note: |§f[^§])".toRegex()

    private var selectedClass = ""

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!LorenzUtils.inSkyBlock || LorenzUtils.skyBlockArea != "Dungeon Hub") return
        if (!config.floorAsStackSize) return

        val itemName = event.stack.name?.removeColor() ?: ""
        val invName = InventoryUtils.openInventoryName()

        if (invName == "Select Floor") {
            if (itemName == "Any") {
                event.stackTip = "A"
            } else if (itemName == "Entrance") {
                event.stackTip = "E"
            } else if (itemName.startsWith("Floor ")) {
                event.stackTip = itemName.split(' ').last().romanToDecimalIfNecessary().toString()
            }
        } else if (itemName.startsWith("The Catacombs - ") || itemName.startsWith("MM Catacombs -")) {
            val floor = itemName.split(" - ").last().removeColor()
            val floorNum = floor.split(' ').last().romanToDecimalIfNecessary().toString()
            val isMasterMode = itemName.contains("MM ")

            event.stackTip = if (floor.contains("Entrance")) {
                "E"
            } else if (isMasterMode) {
                "M${floorNum}"
            } else {
                "F${floorNum}"
            }
        } else if (itemName.endsWith("'s Party")) {
            val floor = event.stack.getLore().find { it.startsWith("§7Floor: ") } ?: return
            val dungeon = event.stack.getLore().find { it.startsWith("§7Dungeon: ") } ?: return
            val floorNum = floor.split(' ').last().romanToDecimalIfNecessary().toString()
            val isMasterMode = dungeon.contains("Master Mode")

            event.stackTip = if (floor.contains("Entrance")) {
                "E"
            } else if (isMasterMode) {
                "M${floorNum}"
            } else {
                "F${floorNum}"
            }
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock || LorenzUtils.skyBlockArea != "Dungeon Hub") return
        if (event.inventoryName != "Catacombs Gate") return

        val lore = event.inventoryItems[45]?.getLore() ?: return

        if (lore[0] == "§7View and select a dungeon class.") {
            selectedClass = lore[2].split(" ").last().removeColor()
        }
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock || LorenzUtils.skyBlockArea != "Dungeon Hub") return
        if (event.gui !is GuiChest) return

        val chest = event.gui.inventorySlots as ContainerChest
        val inventoryName = chest.getInventoryName()
        if (inventoryName != "Party Finder") return

        for (slot in chest.inventorySlots) {
            if (slot == null) continue
            if (slot.slotNumber != slot.slotIndex) continue
            if (slot.stack == null) continue

            val itemName = slot.stack.name ?: continue
            if (!itemName.endsWith(" Party")) continue

            if (config.markIneligibleGroups && slot.stack.getLore().any { ineligiblePattern.matches(it) }) {
                slot highlight LorenzColor.DARK_RED
                continue
            }

            if (config.markPaidCarries) {
                val note = slot.stack.getLore().filter { notePattern.containsMatchIn(it) }.joinToString(" ")

                if (pricePattern.containsMatchIn(note) && carryPattern.containsMatchIn(note)) {
                    slot highlight LorenzColor.RED
                    continue
                }
            }

            if (config.markNonPugs) {
                val note = slot.stack.getLore().filter { notePattern.containsMatchIn(it) }.joinToString(" ")

                if (nonPugPattern.containsMatchIn(note)) {
                    slot highlight LorenzColor.LIGHT_PURPLE
                    continue
                }
            }

            val members = slot.stack.getLore().filter { memberPattern.matches(it) }
            val memberLevels = members.map { memberPattern.matchEntire(it)?.groupValues?.get(2)?.toInt() ?: 0 }
            val memberClasses = members.map { memberPattern.matchEntire(it)?.groupValues?.get(1) ?: "" }

            if (memberLevels.any { it <= config.markBelowClassLevel }) {
                slot highlight LorenzColor.YELLOW
                continue
            }

            if (config.markMissingClass && memberClasses.none { it == selectedClass }) {
                slot highlight LorenzColor.GREEN
            }
        }
    }

    @SubscribeEvent
    fun onItemTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.coloredClassLevel) return

        val chestName = InventoryUtils.openInventoryName()
        if (chestName != "Party Finder") return

        val stack = event.itemStack

        for ((index, line) in stack.getLore().withIndex()) {
            classLevelPattern.matchMatcher(line) {
                val playerName = group("playerName")
                val className = group("className")
                val level = group("level").toInt()
                val color = getColor(level)
                event.toolTip[index + 1] = " §b$playerName§f: §e$className $color$level"
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "dungeon.partyFinderColoredClassLevel", "dungeon.partyFinder.coloredClassLevel")
    }

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
