package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
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

        if (stackSizeConfig.contains(StackSizeMenuConfig.Farming.JACOBS_MEDALS) && ((chestName == "Jacob's Farming Contests") && itemName == ("Claim your rewards!"))) {
            var result = ""
            for (line in item.getLore()) {
                (("(?<colorCode>§.)(?<bold>§l)+(?<medal>[\\w]+) (§.)*(m|M)edals: (§.)*(?<count>[\\w]+)").toPattern()).matchMatcher(line) {
                    result = "$result${group("colorCode")}${group("count")}"
                }
            }
            return result
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Farming.VISITORS_LOGBOOK_COUNTDOWN) && ((chestName == "Visitor's Logbook") && itemName == ("Logbook"))) {
            for (line in item.getLore()) {
                (("(§.)*Next Visitor: (§.)*(?<time>[\\w]+)(m|s).*").toPattern()).matchMatcher(line) {
                    return group("time")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Farming.VISITOR_MILESTONES) && (chestName == "Visitor Milestones")) {
            val lore = item.getLore()
            if (lore.isNotEmpty()) {
                for (line in lore) {
                    (("(§.)*Progress to Tier (?<tier>[\\w]+):.* (§.)*(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%").toPattern()).matchMatcher(line) {
                        return group("percent").replace("100", "§a✔")
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Farming.VISITOR_NPC_RARITIES) && (chestName == "Visitor's Logbook")) {
            val lore = item.getLore()
            for (line in lore) {
                (("§.§(L|l)(UNCOMMON|RARE|LEGENDARY|SPECIAL|MYTHIC)").toPattern()).matchMatcher(line) {
                    return line.take(5)
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Farming.COMPOSTER_INSERT_ABBV)) {
            //(chestName.contains("Composter"))
            //(itemName.contains("Insert ") && itemName.contains(" from "))
            if (chestName == "Composter") {
                (("Insert (?<resource>[\\w]+) from (?<inventorySacks>[\\w]+)").toPattern()).matchMatcher(itemName) {
                    val lore = item.getLore()
                    //§7Totalling §2§240k Fuel§7.
                    //Totalling 40k Fuel.
                    for (line in lore) {
                        composterPattern.matchMatcher(line) {
                            return group("resourceCount")
                        }
                    }
                }
            }
        }

        return ""
    }
}
