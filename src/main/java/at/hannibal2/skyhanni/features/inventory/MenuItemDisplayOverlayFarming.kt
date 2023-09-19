package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayFarming {

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun lazilyGetPercent(original: String, thingToExtract: String = ""): String {
        return original.removeColor().replace(thingToExtract, "").replace("100%", "a✔").take(2).replace(".","").replace("a✔", "§a✔").replace("%","")
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
        ` if (!(InventoryUtils.openInventoryName() == "Visitor's Logbook")) return "" `
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
                        return line.removeColor().replace("Next Visitor: ", "").trim().take(2).replace("s", "").replace("m","")
                    }
                }
            }
        }

        if (stackSizeConfig.contains(2)) {
            if (chestName == "Visitor's Logbook") {
                if (item.getLore() != null) {
                    if (item.getLore().any { it.contains("Times Visited: ") }) {
                        return item.getLore().first().take(5).replace("T", "☉")
                    }
                }
            }
        }

        return ""
    }
}