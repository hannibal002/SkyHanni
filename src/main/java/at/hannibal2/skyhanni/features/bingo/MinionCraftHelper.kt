package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName_old
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import io.github.moulberry.notenoughupdates.recipes.CraftingRecipe
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class MinionCraftHelper {
    private val config get() = SkyHanniMod.feature.event.bingo

    // TODO USE SH-REPO
    private var minionNamePattern = "(?<name>.*) Minion (?<number>.*)".toPattern()
    private var display = emptyList<String>()
    private var hasMinionInInventory = false
    private var hasItemsForMinion = false
    private val tierOneMinions = mutableListOf<String>()
    private val tierOneMinionsDone = mutableListOf<String>()
    private val allIngredients = mutableListOf<String>()
    private val alreadyNotified = mutableListOf<String>()

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        alreadyNotified.clear()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (!config.minionCraftHelperEnabled) return

        if (event.isMod(10)) {
            val mainInventory = Minecraft.getMinecraft()?.thePlayer?.inventory?.mainInventory ?: return
            hasMinionInInventory = mainInventory.mapNotNull { it?.name }.any { isMinionName(it) }
        }

        if (event.repeatSeconds(2)) {
            val mainInventory = Minecraft.getMinecraft()?.thePlayer?.inventory?.mainInventory ?: return
            hasItemsForMinion = loadFromInventory(mainInventory).first.isNotEmpty()
        }

        if (!hasMinionInInventory && !hasItemsForMinion) {
            display = emptyList()
            return
        }

        if (!event.isMod(3)) return

        val mainInventory = Minecraft.getMinecraft()?.thePlayer?.inventory?.mainInventory ?: return

        val (minions, otherItems) = loadFromInventory(mainInventory)

        display = drawDisplay(minions, otherItems)
    }

    private fun drawDisplay(
        minions: MutableMap<String, String>,
        otherItems: MutableMap<String, Int>,
    ): MutableList<String> {
        val newDisplay = mutableListOf<String>()
        for ((minionName, minionId) in minions) {
            minionNamePattern.matchMatcher(minionName) {
                val cleanName = group("name").removeColor()
                val number = group("number").romanToDecimalIfNecessary()
                addMinion(cleanName, number, minionId, otherItems, newDisplay)
            }
        }
        return newDisplay
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        tierOneMinionsDone.clear()
    }

    private fun loadFromInventory(mainInventory: Array<ItemStack?>): Pair<MutableMap<String, String>, MutableMap<String, Int>> {
        init()

        val minions = mutableMapOf<String, String>()
        val otherItems = mutableMapOf<String, Int>()

        for (item in mainInventory) {
            val name = item?.name?.removeColor() ?: continue
            val rawId = item.getInternalName_old()
            if (isMinionName(name)) {
                minions[name] = rawId
            }
        }

        val allMinions = tierOneMinions.toMutableList()
        minions.values.mapTo(allMinions) { it.addOneToId() }

        for (item in mainInventory) {
            val name = item?.name?.removeColor() ?: continue
            val rawId = item.getInternalName_old()
            if (!isMinionName(name)) {
                if (!allIngredients.contains(rawId)) continue
                if (!isAllowed(allMinions, rawId)) continue

                val (itemId, multiplier) = NEUItems.getMultiplier(rawId)
                val old = otherItems.getOrDefault(itemId, 0)
                otherItems[itemId] = old + item.stackSize * multiplier
            }
        }

        firstMinionTier(otherItems, minions)
        return Pair(minions, otherItems)
    }

    private fun isAllowed(allMinions: List<String>, internalName: String): Boolean {
        val a = NEUItems.getMultiplier(internalName)
        for (minion in allMinions) {
            val recipes = NEUItems.getRecipes(minion)

            for (recipe in recipes) {
                for (ingredient in recipe.ingredients) {
                    val ingredientInternalName = ingredient.internalItemId
                    if (ingredientInternalName == internalName) return true

                    val b = NEUItems.getMultiplier(ingredientInternalName)
                    if (a.first == b.first && a.second < b.second) return true
                }
            }
        }
        return false
    }

    private fun init() {
        if (tierOneMinions.isNotEmpty()) return

        allIngredients.clear()

        for (internalId in NotEnoughUpdates.INSTANCE.manager.itemInformation.keys) {
            if (internalId.endsWith("_GENERATOR_1")) {
                if (internalId == "REVENANT_GENERATOR_1") continue
                if (internalId == "TARANTULA_GENERATOR_1") continue
                if (internalId == "VOIDLING_GENERATOR_1") continue
                if (internalId == "INFERNO_GENERATOR_1") continue
                if (internalId == "VAMPIRE_GENERATOR_1") continue
                tierOneMinions.add(internalId)
            }

            if (internalId.contains("_GENERATOR_")) {
                for (recipe in NEUItems.getRecipes(internalId)) {
                    if (recipe !is CraftingRecipe) continue

                    for (ingredient in recipe.ingredients) {
                        val id = ingredient.internalItemId
                        if (!id.contains("_GENERATOR_") && !allIngredients.contains(id)) {
                            allIngredients.add(id)
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
            for (recipe in NEUItems.getRecipes(minionId)) {
                if (recipe !is CraftingRecipe) continue
                if (recipe.ingredients.any { help.contains(it.internalItemId) }) {
                    val name = recipe.output.itemStack.name!!.removeColor()
                    val abc = name.replace(" I", " 0")
                    minions[abc] = minionId.replace("_1", "_0")
                }
            }
        }
    }

    private fun addMinion(
        name: String,
        minionTier: Int,
        minionId: String,
        otherItems: MutableMap<String, Int>,
        newDisplay: MutableList<String>,
    ) {
        val nextTier = minionTier + 1
        val minionName = "§9$name Minion $nextTier"
        newDisplay.add(minionName)
        val nextMinionId = minionId.addOneToId()
        for (recipe in NEUItems.getRecipes(nextMinionId)) {
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
                val (itemId, multiplier) = NEUItems.getMultiplier(rawId)
                val needAmount = need * multiplier
                val have = otherItems.getOrDefault(itemId, 0)
                val percentage = have.toDouble() / needAmount
                val itemName = NEUItems.getItemStack(rawId).name ?: "§cName??§f"
                if (percentage >= 1) {
                    val color = if (itemId.startsWith("WOOD_")) "§7" else "§a"
                    newDisplay.add("  $itemName§8: ${color}DONE")
                    otherItems[itemId] = have - needAmount
                } else {
                    val format = LorenzUtils.formatPercentage(percentage)
                    val haveFormat = LorenzUtils.formatInteger(have)
                    val needFormat = LorenzUtils.formatInteger(needAmount)
                    newDisplay.add("$itemName§8: §e$format §8(§7$haveFormat§8/§7$needFormat§8)")
                    allDone = false
                }
            }
            newDisplay.add(" ")
            if (allDone) {
                addMinion(name, nextTier, nextMinionId, otherItems, newDisplay)
                notify(minionName)
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (!config.minionCraftHelperEnabled) return

        config.minionCraftHelperPos.renderStrings(display, posLabel = "Minion Craft Helper")
    }

    private fun notify(minionName: String) {
        if (alreadyNotified.contains(minionName)) return

        LorenzUtils.sendTitle("Can craft $minionName", 3.seconds)
        alreadyNotified.add(minionName)
    }

    private fun String.addOneToId(): String {
        val lastText = split("_").last()
        val next = lastText.toInt() + 1
        return replace(lastText, "" + next)
    }

    private fun isMinionName(itemName: String) = itemName.contains(" Minion ") && !itemName.contains(" Minion Skin")

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (event.inventoryName != "Crafted Minions") return

        for ((_, b) in event.inventoryItems) {
            val name = b.name ?: continue
            if (!name.startsWith("§e")) continue

            val internalName = NEUItems.getRawInternalName("$name I").replace("MINION", "GENERATOR").replace(";", "_")
            if (!tierOneMinionsDone.contains(internalName)) {
                tierOneMinionsDone.add(internalName)
            }
        }
    }
}
