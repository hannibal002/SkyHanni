package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayCombat {
    private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()


    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.stackSize.menu.combat.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.stackSize.menu.combat
        val chestName = InventoryUtils.openInventoryName()

        if (stackSizeConfig.contains(StackSizeMenuConfig.Combat.BESTIARY_LEVEL)) {
            (("Bestiary.*").toPattern()).matchMatcher(chestName) {
                (("Bestiary Milestone (?<milestone>[\\w]+)").toPattern()).matchMatcher(itemName) {
                        return group("milestone")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Combat.BESTIARY_OVERALL_FAMILY_PROGRESS)) {
            (("Bestiary.*").toPattern()).matchMatcher(chestName) {
                if (itemName.isNotEmpty()) {
                    val lore = item.getLore()
                    for (line in lore) {
                        ((".*(Families Completed|Overall Progress):.* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%.*").toPattern()).matchMatcher(line) {
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
                        (("(§.)*(?<mobType>[\\w]+) Slayer: (§.)*LVL (?<level>[\\w]+)").toPattern()).matchMatcher(line) {
                            return group("level")
                        }
                    }
                }
            }
            if (itemName == ("Boss Leveling Rewards")) {
                for (line in lore) {
                    (("(§.)*Current LVL: (§.)*(?<level>[\\w]+)").toPattern()).matchMatcher(line) {
                        return group("level")
                    }
                }
            }
        }

        if ((stackSizeConfig.contains(StackSizeMenuConfig.Combat.SLAYER_COMBAT_WISDOM_BUFF)) && (itemName == ("Global Combat Wisdom Buff"))) {
            for (line in item.getLore()) {
                (("(§.)*Total buff: (§.)*\\+(?<combatWise>[\\w]+). Combat Wisdom").toPattern()).matchMatcher(line) {
                    return group("combatWise")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Combat.RNG_METER_PROGRESS) && itemName == ("RNG Meter")) {
            for (line in item.getLore()) {
                ((".*(§.)+Progress:.* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%.*").toPattern()).matchMatcher(line) {
                    return group("percent").replace("100", "§a✔")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Combat.UNLOCKED_SLAYER_RECIPES) && itemName == ("Slayer Recipes")) {
            for (line in item.getLore()) {
                ((".*(§.)*Unlocked: (§.)*(?<recipes>[\\w]+) recipes.*").toPattern()).matchMatcher(line) {
                    return group("recipes")
                }
            }
        }

        return ""
    }
}
