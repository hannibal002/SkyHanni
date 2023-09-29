package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayFarming {
    private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\.[0-9]*)?(§.)?%".toPattern()

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.menuItemNumberFarmingAsStackSize.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.menuItemNumberFarmingAsStackSize
        val chestName = InventoryUtils.openInventoryName()
        /*
        -------------------------------IMPORTANT------------------------------------
        
        If at *any* point someone tells you to not nest your code, do the following
        (in any order, but more importantly in the order given below):
        - tell them to "cope and seethe", especially if they cite CodeAesthetic
        - kindly remind them that the last time someone attempted this, friendships
          were almost shattered in the process
        - you will lose your sanity as you try to figure out what the fuck went wrong
          in your forays with string manipulation
        - remind them of the Single-responsibility Principle:
          https://en.wikipedia.org/wiki/Single-responsibility_principle
        - make sure you have an IDE capable of debugging within your reach
        
        This concludes the PSA. Happy writing! -Erymanthus

        PS: T'was all a joke. Just don't do stupid shit like
        ` if (!(chestName == "Visitor's Logbook")) return "" `
        and you *should* be fine for the most part.
        ----------------------------------------------------------------------------
        */

        //NOTE: IT'S String.length, NOT String.length()!

        if (stackSizeConfig.contains(0)) {
            if ((chestName == "Jacob's Farming Contests") && itemName.contains("Claim your rewards!")) {
                var gold = "§60"
                var silver = "§f0"
                var bronze = "§c0"
                for (line in item.getLore()) {
                    var noColorLine = line.removeColor()
                    if (noColorLine.contains("GOLD")) gold = "§6" + noColorLine.split(" ").last()
                    if (noColorLine.contains("SILVER")) silver = "§f" + noColorLine.split(" ").last()
                    if (noColorLine.contains("BRONZE")) bronze = "§c" + noColorLine.split(" ").last()
                }
                return gold + silver + bronze
            }
        }

        if (stackSizeConfig.contains(1)) {
            if ((chestName == "Visitor's Logbook") && itemName == ("Logbook")) {
                for (line in item.getLore()) {
                    if (line.contains("Next Visitor: ")) {
                        return line.removeColor().replace("Next Visitor: ", "").trim().take(2).replace("s", "").replace("m", "")
                    }
                }
            }
        }

        if (stackSizeConfig.contains(2) && (chestName == "Visitor Milestones")) {
            val lore = item.getLore()
            if (!(lore.isEmpty())) {
                if ((lore.any { it.contains("Progress ") }) && (lore.any { it.contains(": ") }) && (lore.any { it.contains("%") })) {
                    for (line in lore) {
                        if (line.contains("Progress ") && line.contains(": ") && line.contains("%")) {
                            return genericPercentPattern.matchMatcher(line) { group("percent").replace("100", "§a✔") } ?: ""
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(3)) {
            val lore = item.getLore()
            if (chestName == "Visitor's Logbook") {
                if (!(lore.isEmpty())) {
                    if (lore.any { it.contains("Times Visited: ") }) {
                        return lore.first().take(5).replace("T", "☉")
                    }
                }
            }
        }

        if (stackSizeConfig.contains(4) && (chestName.contains("Composter"))) {
            val lore = item.getLore()
            if (itemName.contains("Insert ") && itemName.contains(" from ")) {
                if ((lore.any { (it.contains("Totalling ")) })) {
                    for (line in lore) {
                        if (line.contains("Totalling ")) {
                            //§7Totalling §e§e615 Organic Matter§7.
                            //§7Totalling §e§e844 Organic Matter§7.
                            //Totalling 615 Organic Matter.
                            //Totalling 844 Organic Matter.
                            if (itemName.contains(" Crops ")) {
                                return line.removeColor().between("Totalling ", " Organic Matter.")
                            }
                            if (itemName.contains(" Fuel ")) {
                                return line.removeColor().between("Totalling ", " Fuel.")
                            }
                        }
                    }
                }
            }
        }

        return ""
    }
}