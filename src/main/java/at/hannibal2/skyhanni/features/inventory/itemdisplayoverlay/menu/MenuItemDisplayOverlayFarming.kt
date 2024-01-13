package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.AbstractMenuStackSize
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayFarming : AbstractMenuStackSize() {
    private val farmingSubgroup = itemStackSizeGroup.group("farming")
    
    private val totallingResourcePattern by farmingSubgroup.pattern(
        "totalling.resource.loreline",
        ".*(§.)Totalling ((§.)+)(?<resourceCount>[\\w]+) (?<resourceType>[ \\w]+)(§.)\\..*"
    )
    private val jacobMedalInventoryPattern by farmingSubgroup.pattern(
        "jacob.medal.inventory.loreline",
        "(?<colorCode>§.)(?<bold>§l)+(?<medal>[\\w]+) (§.)*(m|M)edals: (§.)*(?<count>[\\w]+)"
    )
    private val nextVisitorPattern by farmingSubgroup.pattern(
        "next.visitor.loreline",
        "(§.)*Next Visitor: (§.)*(?<time>[\\w]+)(m|s).*"
    )
    private val insertFromPattern by farmingSubgroup.pattern(
        "insert.from.itemname",
        "Insert (?<resource>[\\w]+) from (?<inventorySacks>[\\w]+)"
    )
    private val visitorRarityPattern by farmingSubgroup.pattern(
        "visitor.rarity.loreline",
        "§.§(L|l)(UNCOMMON|RARE|LEGENDARY|SPECIAL|MYTHIC)"
    )
    private val visitorMilestonePercentPattern by farmingSubgroup.pattern(
        "visitor.milestone.percent.loreline",
        "(§.)*Progress to Tier (?<tier>[\\w]+):.* (§.)*(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"
    )

    @SubscribeEvent
    override fun onRenderItemTip(event: RenderItemTipEvent) {
        super.onRenderItemTip(event)
    }

    override fun getStackTip(item: ItemStack): String {
        if (configMenuStackSize.farming.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = configMenuStackSize.farming
        val chestName = InventoryUtils.openInventoryName()
        val lore = item.getLore()
        
        if (lore.isEmpty()) return ""

        if (stackSizeConfig.contains(StackSizeMenuConfig.Farming.JACOBS_MEDALS) && ((chestName == "Jacob's Farming Contests") && itemName == ("Claim your rewards!"))) {
            var result = ""
            for (line in lore) {
                jacobMedalInventoryPattern.matchMatcher(line) {
                    result = "$result${group("colorCode")}${group("count")}"
                }
            }
            return result
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Farming.VISITORS_LOGBOOK_COUNTDOWN) && ((chestName == "Visitor's Logbook") && itemName == ("Logbook"))) {
            for (line in lore) {
                nextVisitorPattern.matchMatcher(line) {
                    return group("time")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Farming.VISITOR_MILESTONES) && (chestName == "Visitor Milestones")) {
            for (line in lore) {
                visitorMilestonePercentPattern.matchMatcher(line) {
                    return group("percent").convertPercentToGreenCheckmark()
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Farming.VISITOR_NPC_RARITIES) && (chestName == "Visitor's Logbook")) {
            for (line in lore) {
                visitorRarityPattern.matchMatcher(line) {
                    return line.take(5)
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Farming.COMPOSTER_INSERT_ABBV)) {
            if (chestName == "Composter") {
                insertFromPattern.matchMatcher(itemName) {
                    for (line in lore) {
                        totallingResourcePattern.matchMatcher(line) {
                            return group("resourceCount")
                        }
                    }
                }
            }
        }

        return ""
    }
}
