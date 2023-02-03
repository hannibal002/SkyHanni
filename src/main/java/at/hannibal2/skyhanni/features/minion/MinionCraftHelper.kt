package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import io.github.moulberry.notenoughupdates.recipes.CraftingRecipe
import io.github.moulberry.notenoughupdates.recipes.NeuRecipe
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.regex.Pattern

class MinionCraftHelper {

    private var minionNamePattern = Pattern.compile("(.*) Minion (.*)")

    var tick = 0
    var display = mutableListOf<String>()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.minions.minionCraftHelperEnabled) return

        tick++
        if (tick % 5 != 0) return

        val mainInventory = Minecraft.getMinecraft()?.thePlayer?.inventory?.mainInventory ?: return

        val minions = mutableMapOf<String, String>()
        val otherItems = mutableMapOf<String, Int>()

        for (item in mainInventory) {
            val name = item?.name?.removeColor() ?: continue
            if (name.contains(" Minion ")) {
                val minionId = NotEnoughUpdates.INSTANCE.manager.createItemResolutionQuery()
                    .withItemStack(item)
                    .resolveInternalName() ?: continue
                minions[name] = minionId
            } else {
                val (itemName, multiplier) = getMultiplier(name)
                val old = otherItems.getOrDefault(itemName, 0)
                otherItems[itemName] = old + item.stackSize * multiplier
            }
        }

        display.clear()
        for ((minionName, minionId) in minions) {
            val matcher = minionNamePattern.matcher(minionName)
            if (!matcher.matches()) return
            val cleanName = matcher.group(1).removeColor()
            val number = matcher.group(2).romanToDecimal()
            addMinion(cleanName, number, minionId, otherItems)
        }
    }

    private fun getMultiplier(name: String) = if (name.startsWith("Enchanted")) {
        Pair(name.substring(10), 160)
    } else {
        Pair(name, 1)
    }

    private fun addMinion(
        minionName: String,
        minionNumber: Int,
        minionId: String,
        otherItems: MutableMap<String, Int>
    ) {
        val nextNumber = minionNumber + 1
        display.add("$minionName Minion $minionNumber -> $nextNumber")
        val recipes: List<NeuRecipe> = NotEnoughUpdates.INSTANCE.manager.getAvailableUsagesFor(minionId)
        for (recipe in recipes) {
            if (recipe !is CraftingRecipe) continue
            val output = recipe.output
            val internalItemId = output.internalItemId
            if (!internalItemId.contains("_GENERATOR_")) continue
            val map = mutableMapOf<String, Int>()
            for (input in recipe.inputs) {
                val itemId = input.internalItemId
                if (minionId != itemId) {
                    val itemName = input.itemStack.name?.removeColor()!!
                    val count = input.count.toInt()
                    val old = map.getOrDefault(itemName, 0)
                    map[itemName] = old + count
                }
            }
            for ((name, need) in map) {
                val (itemName, multiplier) = getMultiplier(name)
                val needAmount = need * multiplier
                val have = otherItems.getOrDefault(itemName, 0)
                val percentage = have.toDouble() / needAmount
                if (percentage >= 1) {
                    display.add("  $itemName§8: §aDONE")
                    display.add(" ")
                    otherItems[itemName] = have - needAmount
                    addMinion(minionName, minionNumber + 1, minionId.addOneToId(), otherItems)
                } else {
                    val format = LorenzUtils.formatPercentage(percentage)
                    val haveFormat = LorenzUtils.formatInteger(have)
                    val needFormat = LorenzUtils.formatInteger(needAmount)
                    display.add("$itemName§8: §e$format §8(§7$haveFormat§8/§7$needFormat§8)")
                    display.add(" ")
                }
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.minions.minionCraftHelperEnabled) return

        SkyHanniMod.feature.minions.minionCraftHelperPos.renderStrings(display, center = true)
    }
}

private fun String.addOneToId(): String {
    val split = split("_")
    val lastText = split.last()
    val next = lastText.toInt() + 1
    val result = replace(lastText, "" + next)
    return result
}
