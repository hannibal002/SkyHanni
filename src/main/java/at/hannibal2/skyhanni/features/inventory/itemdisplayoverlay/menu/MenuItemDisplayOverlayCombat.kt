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

class MenuItemDisplayOverlayCombat : AbstractMenuStackSize() {
    // private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()
    private val bestiaryChestNamePattern = (("Bestiary.*").toPattern())
    private val bestiaryMilestoneItemNamePattern = (("Bestiary Milestone (?<milestone>[\\w]+)").toPattern())
    private val familiesCompletedOverallProgressPercentLoreLinePattern = ((".*(Families Completed|Overall Progress):.* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%.*").toPattern())
    private val slayerLevelLoreLinePattern = (("(§.)*(?<mobType>[\\w]+) Slayer: (§.)*LVL (?<level>[\\w]+)").toPattern())
    private val slayerLevelOtherLoreLinePattern = (("(§.)*Current LVL: (§.)*(?<level>[\\w]+)").toPattern())
    private val combatWisdomBuffLoreLinePattern = (("(§.)*Total buff: (§.)*\\+(?<combatWise>[\\w]+). Combat Wisdom").toPattern())
    private val rngMeterProgressPercentLoreLinePattern = ((".*(§.)+Progress:.* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%.*").toPattern())
    private val unlockedSlayerRecipesLoreLinePattern = ((".*(§.)*Unlocked: (§.)*(?<recipes>[\\w]+) recipes.*").toPattern())
    private val overallProgressToggleLoreLinePattern = (("(§.)*Overall Progress: (§.)*(?<status>[\\w]+)").toPattern())

    @SubscribeEvent
    override fun onRenderItemTip(event: RenderItemTipEvent) {
        super.onRenderItemTip(event)
    }

    override fun getStackTip(item: ItemStack): String {
        if (configMenuStackSize.combat.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = configMenuStackSize.combat
        val chestName = InventoryUtils.openInventoryName()

        if (stackSizeConfig.contains(StackSizeMenuConfig.Combat.BESTIARY_LEVEL)) {
            bestiaryChestNamePattern.matchMatcher(chestName) {
                bestiaryMilestoneItemNamePattern.matchMatcher(itemName) {
                        return group("milestone")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Combat.BESTIARY_OVERALL_FAMILY_PROGRESS)) {
            bestiaryChestNamePattern.matchMatcher(chestName) {
                val lore = item.getLore()
                if (itemName == "Toggle Families Completed Display") {
                    for (line in lore) {
                        overallProgressToggleLoreLinePattern.matchMatcher(line) {
                            if (group("status") != "SHOWN") return "§c‼‼‼"
                        }
                    }
                }
                if (itemName.isNotEmpty()) {
                    for (line in lore) {
                        familiesCompletedOverallProgressPercentLoreLinePattern.matchMatcher(line) {
                            return group("percent").replace("100", "§a✔")
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Combat.SLAYER_LEVELS)) {
            val lore = item.getLore()
            if (chestName == ("Slayer")) {
                if (itemName.isNotEmpty() && lore.isNotEmpty()) {
                    for (line in lore) {
                        slayerLevelLoreLinePattern.matchMatcher(line) {
                            return group("level")
                        }
                    }
                }
            }
            if (itemName == ("Boss Leveling Rewards")) {
                for (line in lore) {
                    slayerLevelOtherLoreLinePattern.matchMatcher(line) {
                        return group("level")
                    }
                }
            }
        }

        if ((stackSizeConfig.contains(StackSizeMenuConfig.Combat.SLAYER_COMBAT_WISDOM_BUFF)) && (itemName == ("Global Combat Wisdom Buff"))) {
            for (line in item.getLore()) {
                combatWisdomBuffLoreLinePattern.matchMatcher(line) {
                    return group("combatWise")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Combat.RNG_METER_PROGRESS) && itemName == ("RNG Meter")) {
            for (line in item.getLore()) {
                rngMeterProgressPercentLoreLinePattern.matchMatcher(line) {
                    return group("percent").replace("100", "§a✔")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Combat.UNLOCKED_SLAYER_RECIPES) && itemName == ("Slayer Recipes")) {
            for (line in item.getLore()) {
                unlockedSlayerRecipesLoreLinePattern.matchMatcher(line) {
                    return group("recipes")
                }
            }
        }

        return ""
    }
}
