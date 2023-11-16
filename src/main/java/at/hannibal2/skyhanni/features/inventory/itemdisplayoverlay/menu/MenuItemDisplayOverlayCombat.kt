package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.InventoryConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayCombat {
    private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()


    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getTheLastWordNoColor(original: String): String {
        return original.removeColor().split(" ").last()
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.stackSize.menu.combat.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.stackSize.menu.combat
        val chestName = InventoryUtils.openInventoryName()

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.Combat.BESTIARY_LEVEL) && ((chestName.contains("Bestiary")) && itemName.isNotEmpty() && (itemName.contains("Bestiary Milestone ")))) {
            return itemName.split(" ").last()
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.Combat.BESTIARY_OVERALL_FAMILY_PROGRESS) && ((chestName.contains("Bestiary")) && itemName.isNotEmpty())) {
            val lore = item.getLore()
            for (line in lore) {
                if (line.contains("Families Completed: ") || line.contains("Overall Progress: ")) {
                    return genericPercentPattern.matchMatcher(line.removeColor().replace(" (MAX!)", "")) { group("percent").replace("100", "§a✔") } ?: ""
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.Combat.SLAYER_LEVELS)) {
            if ((chestName.contains("Slayer")) && itemName.isNotEmpty()) {
                val lore = item.getLore()
                for (line in lore) {
                    if (line.contains(" Slayer: ")) {
                        return getTheLastWordNoColor(line)
                    }
                }
            }
            if (itemName.contains("Boss Leveling Rewards")) {
                val lore = item.getLore()
                for (line in lore) {
                    if (line.contains("Current LVL: ")) {
                        return getTheLastWordNoColor(line)
                    }
                }
            }
        }

        if ((stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.Combat.SLAYER_COMBAT_WISDOM_BUFF)) && (itemName.contains("Global Combat Wisdom"))) {
            for (line in item.getLore()) {
                if (line.contains("Total buff")) {
                    return "§3" + line.removeColor().replace("Total buff: +", "").replace("☯ Combat Wisdom", "")
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.Combat.RNG_METER_PROGRESS) && itemName.contains("RNG Meter")) {
            for (line in item.getLore()) {
                if (line.contains("Progress: ")) {
                    return genericPercentPattern.matchMatcher(line) { group("percent").replace("100", "§a✔") } ?: ""
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.Combat.UNLOCKED_SLAYER_RECIPES) && itemName.contains("Slayer Recipes")) {
            for (line in item.getLore()) {
                if (line.contains("Unlocked: ")) {
                    return line.removeColor().lowercase().split("unlocked: ", " recipes")[1] //need to use lowercase here because one day the admins are going to capitalize the word "recipes" in "Unlocked: 26 Recipes" and it'll break this feature i may as well futureproof it right now
                }
            }
        }

        return ""
    }
}
