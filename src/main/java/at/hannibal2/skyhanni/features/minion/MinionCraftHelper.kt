package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.recipes.CraftingRecipe
import io.github.moulberry.notenoughupdates.recipes.NeuRecipe
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.regex.Pattern

class MinionCraftHelper {

    private var minionNamePattern = Pattern.compile("(.*) Minion (.*)")
    private var tick = 0
    private var display = mutableListOf<String>()
    private var hasMinionInInventory = false

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.minions.minionCraftHelperEnabled) return

        tick++

        if (tick % 60 == 0) {
            val mainInventory = Minecraft.getMinecraft()?.thePlayer?.inventory?.mainInventory ?: return
            hasMinionInInventory = mainInventory
                .mapNotNull { it?.name?.removeColor() }
                .any { it.contains(" Minion ") }
        }

        if (!hasMinionInInventory) return

        if (tick % 5 != 0) return
//        if (tick % 60 != 0) return

        val mainInventory = Minecraft.getMinecraft()?.thePlayer?.inventory?.mainInventory ?: return

        val minions = mutableMapOf<String, String>()
        val otherItems = mutableMapOf<String, Int>()

        for (item in mainInventory) {
            val name = item?.name?.removeColor() ?: continue
            val rawId = item.getInternalName()
            if (name.contains(" Minion ")) {
                minions[name] = rawId
            } else {
                val (itemId, multiplier) = getMultiplier(rawId)
                val old = otherItems.getOrDefault(itemId, 0)
                otherItems[itemId] = old + item.stackSize * multiplier
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

    private fun addMinion(
        minionName: String,
        minionNumber: Int,
        minionId: String,
        otherItems: MutableMap<String, Int>
    ) {
        val nextNumber = minionNumber + 1
        display.add("$minionName Minion $minionNumber -> $nextNumber")
        val recipes: List<NeuRecipe> = NEUItems.manager.getAvailableUsagesFor(minionId)
        for (recipe in recipes) {
            if (recipe !is CraftingRecipe) continue
            val output = recipe.output
            val internalItemId = output.internalItemId
            if (!internalItemId.contains("_GENERATOR_")) continue
            val map = mutableMapOf<String, Int>()
            for (input in recipe.inputs) {
                val itemId = input.internalItemId
                if (minionId != itemId) {
                    val count = input.count.toInt()
                    val old = map.getOrDefault(itemId, 0)
                    map[itemId] = old + count
                }
            }
            for ((rawId, need) in map) {
//                println("need rawId: $rawId")
                val (itemId, multiplier) = getMultiplier(rawId)
                val needAmount = need * multiplier
                val have = otherItems.getOrDefault(itemId, 0)
                val percentage = have.toDouble() / needAmount
                val itemName = NEUItems.getItemStack(rawId).name ?: "§cName??§f"
                if (percentage >= 1) {
                    display.add("  $itemName§8: §aDONE")
                    display.add(" ")
                    otherItems[itemId] = have - needAmount
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
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.minions.minionCraftHelperEnabled) return

        SkyHanniMod.feature.minions.minionCraftHelperPos.renderStrings(display, center = true)
    }
}

private fun String.addOneToId(): String {
    val lastText = split("_").last()
    val next = lastText.toInt() + 1
    return replace(lastText, "" + next)
}

private fun getMultiplier(rawId: String): Pair<String, Int> {
    if (rawId == "MELON_BLOCK") {
        return Pair("MELON", 9)
    }
    if (rawId == "HAY_BLOCK") {
        return Pair("WHEAT", 9)
    }
    if (rawId == "ENCHANTED_HAY_BLOCK") {
        return Pair("WHEAT", 9 * 16 * 9)
    }
    if (rawId == "PACKED_ICE") {
        return Pair("ICE", 9)
    }
    if (rawId == "ENCHANTED_MITHRIL") {
        return Pair("MITHRIL_ORE", 160)
    }
    if (rawId == "ENCHANTED_GOLD") {
        return Pair("GOLD_INGOT", 160)
    }
    return if (rawId.startsWith("ENCHANTED_")) {
        Pair(rawId.substring(10), 160)
    } else {
        Pair(rawId, 1)
    }
}
