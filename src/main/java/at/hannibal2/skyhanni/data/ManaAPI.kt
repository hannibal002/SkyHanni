package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ActionBarValueUpdate
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ManaChangeEvent
import at.hannibal2.skyhanni.events.item.ItemAbilityCastEvent
import at.hannibal2.skyhanni.features.itemabilities.abilitycooldown.ItemAbilityType
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getManaDisintegrators
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

object ManaAPI {

    var estimatedMana = -1.0
    var maxMana = -1.0

    private var lastHypixelMana = -1.0

    var lastAbilityCastTime = SimpleTimeMark.farPast()
    var lastAbilityCastText = ""

    val regenPerTick get() = maxMana / 50.0

    @SubscribeEvent
    fun onActionBarValueUpdate(event: ActionBarValueUpdate) {
        if (event.updated == ActionBarStatsData.MANA) {
            val mana = event.updated.value.formatNumber().toDouble()
            if (estimatedMana == -1.0) {
                estimatedMana = mana
            }
            if (lastHypixelMana != mana) {
                lastHypixelMana = mana
                hypixelManaChange(lastHypixelMana, mana)
            }
        }
        if (event.updated == ActionBarStatsData.MAX_MANA) {
            maxMana = event.updated.value.formatNumber().toDouble()
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val speedFactor = 4
        if (event.isMod(40 / speedFactor)) {
            val regenPerTick = (regenPerTick / speedFactor).toInt()
//             println("regenPerTick: ${regenPerTick.addSeparators()} (maxMana=${maxMana.addSeparators()})")
            estimatedMana += regenPerTick
            update()
        }
    }

    private fun update() {
        if (estimatedMana < 0) {
            estimatedMana = 0.0
        }
        if (estimatedMana > maxMana) {
            estimatedMana = maxMana
        }
        ManaChangeEvent().postAndCatch()
    }

    @SubscribeEvent
    fun onItemAbilityCast(event: ItemAbilityCastEvent) {
        val itemStack = event.itemStack
        val cost = manaCost(event.itemAbility, itemStack)
        estimatedMana -= cost

        val itemName = itemStack.getInternalName().getItemName().removeColor()

//         lastAbilityCastText = "§c- ${diff.addSeparators()} §7(${event.itemAbility})"
        lastAbilityCastText = "§c- ${cost.addSeparators()} §7(${itemName})"
        lastAbilityCastTime = SimpleTimeMark.now()
    }

    private fun manaCost(itemAbility: ItemAbilityType, itemStack: ItemStack): Double {
        val manaCostPercentage = itemAbility.manaCostPercentage
        if (manaCostPercentage != 0.0) {
            return maxMana * manaCostPercentage
        }

        val baseCost = if (IslandType.THE_RIFT.isInIsland()) {
            itemAbility.riftManaCost
        } else itemAbility.manaCost

        var reduceFactor = 0.0
        itemStack.getEnchantments()?.let {
            val ultimateWiseLevel = it["ultimate_wise"] ?: 0
            reduceFactor += ultimateWiseLevel * 0.1
        }

        itemStack.getManaDisintegrators()?.let {
            reduceFactor += it * 0.01
        }
        return baseCost * (1 - reduceFactor)
    }

    private fun hypixelManaChange(old: Double, new: Double) {
        println("hypixel mana change: ${old.addSeparators()} -> ${new.addSeparators()}")
        if (lastAbilityCastTime.passedSince() > 2.seconds) {
            val distance = abs(new - estimatedMana)
            if (distance > regenPerTick * 2) {
                // reset to known value
                ChatUtils.debug("Fixed wrong estimated mana value")
//                 ErrorManager.logErrorStateWithData(
//                     "Fixed wrong estimated mana value",
//                     "estimatedMana is wrong",
//                     "estimatedMana" to estimatedMana,
//                     "new" to new,
//                     "distance" to distance,
//                     "regenPerTick" to regenPerTick,
//                     "old" to old,
//                 )
                estimatedMana = new
            }
        }
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        estimatedMana = -1.0
        lastHypixelMana = -1.0
        maxMana = -1.0
    }
}
