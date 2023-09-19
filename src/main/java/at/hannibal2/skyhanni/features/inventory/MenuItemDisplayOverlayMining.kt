package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayMining {

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun lazilyGetPercent(original: String, thingToExtract: String = ""): String {
        return original.removeColor().replace(thingToExtract, "").replace("100%", "a✔").take(2).replace(".","").replace("a✔", "§a✔").replace("%","")
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.menuItemNumberMiningAsStackSize.isEmpty()) return ""
        val stackSizeConfig = SkyHanniMod.feature.inventory.menuItemNumberMiningAsStackSize
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
            if (chestName == "Heart of the Mountain") {
                val nameWithColor = item.name ?: return ""
                if ((nameWithColor.startsWith("§a")) || (nameWithColor.startsWith("§e")) || (nameWithColor.startsWith("§c"))) {
                    val lore = item.getLore()
                    if ((lore.firstOrNull() == null) || (lore.lastOrNull() == null)) return ""
                    if (!lore.first().contains("Level ") && !lore.last().contains("Right click to ")) return ""
                    if (lore.last().contains("the Mountain!") || lore.last().contains("Requires ")) return ""
                    var level = lore.first().removeColor().replace("Level ", "")
                    var colorCode = ""
                    if (level.contains("/")) level = level.split("/")[0]
                    if (nameWithColor.startsWith("§a")) level = "✔"
                    if (lore.last().removeColor().replace("Right click to ","").contains("enable")) colorCode = "§c"
                    return "" + colorCode + level
                }
            }
        }

        if (stackSizeConfig.contains(1)) {
            if (chestName == "Heart of the Mountain") {
                val nameWithColor = item.name ?: return ""
                if (nameWithColor != "§5Crystal Hollows Crystals") return ""
                val lore = item.getLore()
                var crystalsNotPlaced = 0
                var crystalsNotFound = 0
                val totalCrystals = 5 //change "5" to whatever new value Hypixel does if this value ever changes
                for (line in lore) {
                    if (line.contains(" §e✖ Not Placed")) crystalsNotPlaced++
                    else if (line.contains(" §c✖ Not Found")) crystalsNotFound++
                }
                var crystalsPlaced = totalCrystals - crystalsNotPlaced - crystalsNotFound
                return "§a${crystalsPlaced}§r§e${crystalsNotPlaced}§r§c${crystalsNotFound}"
            }
        }

        return ""
    }
}