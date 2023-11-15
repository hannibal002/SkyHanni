package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.InventoryConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayFarming {
    private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()
    private val composterPattern = ".*(§.)Totalling ((§.)+)(?<resourceCount>[\\w]+) (?<resourceType>[ \\w]+)(§.)\\..*".toPattern()

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.stackSize.menu.farming.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.stackSize.menu.farming
        val chestName = InventoryUtils.openInventoryName()

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.Farming.JACOBS_MEDALS) && ((chestName == "Jacob's Farming Contests") && itemName.contains("Claim your rewards!"))) {
            var gold = "§60"
            var silver = "§f0"
            var bronze = "§c0"
            for (line in item.getLore()) {
                val noColorLine = line.removeColor()
                if (noColorLine.contains("GOLD")) gold = "§6" + noColorLine.split(" ").last()
                if (noColorLine.contains("SILVER")) silver = "§f" + noColorLine.split(" ").last()
                if (noColorLine.contains("BRONZE")) bronze = "§c" + noColorLine.split(" ").last()
            }
            return gold + silver + bronze
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.Farming.VISITORS_LOGBOOK_COUNTDOWN) && ((chestName == "Visitor's Logbook") && itemName == ("Logbook"))) {
            for (line in item.getLore()) {
                if (line.contains("Next Visitor: ")) {
                    return line.removeColor().replace("Next Visitor: ", "").trim().take(2).replace("s", "").replace("m", "")
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.Farming.VISITOR_MILESTONES) && (chestName == "Visitor Milestones")) {
            val lore = item.getLore()
            if (lore.isNotEmpty()) {
                for (line in lore) {
                    if (line.contains("Progress ") && line.contains(": ") && line.contains("%")) {
                        return genericPercentPattern.matchMatcher(line) { group("percent").replace("100", "§a✔") } ?: ""
                    }
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.Farming.VISITOR_NPC_RARITIES) && (chestName == "Visitor's Logbook")) {
            val lore = item.getLore()
            for (line in lore) {
                if (line.startsWith("§7Times Visited: ")) {
                    return lore.first().take(5).replace("T", "☉")
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.Farming.COMPOSTER_INSERT_ABBV) && (chestName.contains("Composter")) && (itemName.contains("Insert ") && itemName.contains(" from "))) {
            val lore = item.getLore()
            //§7Totalling §2§240k Fuel§7.
            //Totalling 40k Fuel.
            for (line in lore) {
                composterPattern.matchMatcher(line) {
                    return group("resourceCount")
                }
            }
        }

        return ""
    }
}
