package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SendTitleHelper
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import io.github.moulberry.notenoughupdates.recipes.CraftingRecipe
import io.github.moulberry.notenoughupdates.recipes.NeuRecipe
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.regex.Pattern

class MinionCraftHelper {

    private var minionNamePattern = Pattern.compile("(.*) Minion (.*)")
    private var tick = 0
    private var display = mutableListOf<String>()
    private var hasMinionInInventory = false
    private var hasItemsForMinion = false
    private val tierOneMinions = mutableListOf<String>()
    private val tierOneMinionsDone = mutableListOf<String>()
    private val allIngredients = mutableListOf<String>()
    private val alreadyNotified = mutableListOf<String>()
    private val recipesCache = mutableMapOf<String, List<NeuRecipe>>()
    private var multiplierCache = mutableMapOf<String, Pair<String, Int>>()

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        alreadyNotified.clear()
        recipesCache.clear()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.minions.minionCraftHelperEnabled) return

        tick++

        if (tick % 10 == 0) {
            val mainInventory = Minecraft.getMinecraft()?.thePlayer?.inventory?.mainInventory ?: return
            hasMinionInInventory = mainInventory
                .mapNotNull { it?.name?.removeColor() }
                .any { it.contains(" Minion ") }
        }

        if (tick % (60 * 2) == 0) {
            val mainInventory = Minecraft.getMinecraft()?.thePlayer?.inventory?.mainInventory ?: return
            hasItemsForMinion = loadFromInventory(mainInventory).first.isNotEmpty()
        }

        if (!hasMinionInInventory && !hasItemsForMinion) {
            display.clear()
            return
        }

        if (tick % 3 != 0) return
//        if (tick % 60 != 0) return

        val mainInventory = Minecraft.getMinecraft()?.thePlayer?.inventory?.mainInventory ?: return

        val (minions, otherItems) = loadFromInventory(mainInventory)

        display.clear()
        for ((minionName, minionId) in minions) {
            val matcher = minionNamePattern.matcher(minionName)
            if (!matcher.matches()) return
            val cleanName = matcher.group(1).removeColor()
            val number = matcher.group(2).romanToDecimalIfNeeded()
            addMinion(cleanName, number, minionId, otherItems)
        }
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        tierOneMinionsDone.clear()
    }

    private fun loadFromInventory(mainInventory: Array<ItemStack>): Pair<MutableMap<String, String>, MutableMap<String, Int>> {
        init()

        val minions = mutableMapOf<String, String>()
        val otherItems = mutableMapOf<String, Int>()

        for (item in mainInventory) {
            val name = item?.name?.removeColor() ?: continue
            val rawId = item.getInternalName()
            if (name.contains(" Minion ")) {
                minions[name] = rawId
            } else {
                if (!allIngredients.contains(rawId)) continue
                val (itemId, multiplier) = getMultiplier(rawId)
                val old = otherItems.getOrDefault(itemId, 0)
                otherItems[itemId] = old + item.stackSize * multiplier
            }
        }
        firstMinionTier(otherItems, minions)
        return Pair(minions, otherItems)
    }

    private fun init() {
        if (tierOneMinions.isNotEmpty()) return

        allIngredients.clear()

        for (internalId in NotEnoughUpdates.INSTANCE.manager.itemInformation.keys) {
            if (internalId.endsWith("_GENERATOR_1")) {
                tierOneMinions.add(internalId)
            }

            if (internalId.contains("_GENERATOR_")) {
                for (recipe in getRecipes(internalId)) {
                    if (recipe !is CraftingRecipe) continue

                    for (ingredient in recipe.ingredients) {
                        val id = ingredient.internalItemId
                        if (!id.contains("_GENERATOR_")) {
                            if (!allIngredients.contains(id)) {
                                allIngredients.add(id)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun firstMinionTier(otherItems: Map<String, Int>, minions: MutableMap<String, String>) {
        val help = otherItems.filter { !it.key.startsWith("WOOD_") }
        val tierOneMinionsFiltered = tierOneMinions.filter { it !in tierOneMinionsDone }
        for (minionId in tierOneMinionsFiltered) {
            val prefix = minionId.dropLast(1)
            if (minions.any { it.value.startsWith(prefix) }) {
                tierOneMinionsDone.add(minionId)
            }
        }
        for (minionId in tierOneMinionsFiltered) {

            for (recipe in getRecipes(minionId)) {
                if (recipe !is CraftingRecipe) continue
                if (recipe.ingredients.any { help.contains(it.internalItemId) }) {
                    val name = recipe.output.itemStack.name!!.removeColor()
                    val abc = name.replace(" I", " 0")
                    minions[abc] = minionId.replace("_1", "_0")
                }
            }
        }
    }

    private fun addMinion(name: String, minionTier: Int, minionId: String, otherItems: MutableMap<String, Int>) {
        val nextTier = minionTier + 1
        val minionName = "§9$name Minion $nextTier"
        display.add(minionName)
        val nextMinionId = minionId.addOneToId()
        val recipes: List<NeuRecipe> = getRecipes(nextMinionId)
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
            var allDone = true
            for ((rawId, need) in map) {
                val (itemId, multiplier) = getMultiplier(rawId)
                val needAmount = need * multiplier
                val have = otherItems.getOrDefault(itemId, 0)
                val percentage = have.toDouble() / needAmount
                val itemName = NEUItems.getItemStack(rawId).name ?: "§cName??§f"
                if (percentage >= 1) {
                    display.add("  $itemName§8: §aDONE")
                    otherItems[itemId] = have - needAmount
                } else {
                    val format = LorenzUtils.formatPercentage(percentage)
                    val haveFormat = LorenzUtils.formatInteger(have)
                    val needFormat = LorenzUtils.formatInteger(needAmount)
                    display.add("$itemName§8: §e$format §8(§7$haveFormat§8/§7$needFormat§8)")
                    allDone = false
                }
            }
            display.add(" ")
            if (allDone) {
                addMinion(name, nextTier, nextMinionId, otherItems)
                notify(minionName)
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.minions.minionCraftHelperEnabled) return

        SkyHanniMod.feature.minions.minionCraftHelperPos.renderStrings(display, center = true)
    }

    private fun getRecipes(minionId: String): List<NeuRecipe> {
        if (recipesCache.contains(minionId)) {
            return recipesCache[minionId]!!
        }
        val recipes = NEUItems.manager.getAvailableRecipesFor(minionId)
        recipesCache[minionId] = recipes
        return recipes
    }

    private fun notify(minionName: String) {
        if (alreadyNotified.contains(minionName)) return

        SendTitleHelper.sendTitle("Can craft $minionName", 3_000)
        alreadyNotified.add(minionName)
    }

    private fun String.addOneToId(): String {
        val lastText = split("_").last()
        val next = lastText.toInt() + 1
        return replace(lastText, "" + next)
    }


    private fun getMultiplier(rawId: String, tryCount: Int = 0, parent: String? = null): Pair<String, Int> {
        if (multiplierCache.contains(rawId)) {
            return multiplierCache[rawId]!!
        }
        if (tryCount == 10) {
            val message = "Error reading getMultiplier for item '$rawId'"
            Error(message).printStackTrace()
            LorenzUtils.error(message)
            return Pair(rawId, 1)
        }
        for (recipe in getRecipes(rawId)) {
            if (recipe !is CraftingRecipe) continue

            val map = mutableMapOf<String, Int>()
            for (ingredient in recipe.ingredients) {
                val count = ingredient.count.toInt()
                val internalItemId = ingredient.internalItemId
                val old = map.getOrDefault(internalItemId, 0)
                map[internalItemId] = old + count
            }
            if (map.size != 1) continue
            val current = map.iterator().next().toPair()
            val id = current.first
            return if (id != parent) {
                val child = getMultiplier(id, tryCount + 1, rawId)
                val result = Pair(child.first, child.second * current.second)
                multiplierCache[rawId] = result
                result
            } else {
                Pair(parent, 1)
            }
        }

        val result = Pair(rawId, 1)
        multiplierCache[rawId] = result
        return result
    }
}
