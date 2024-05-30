package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.hasEnchantments
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getCachedIngredients
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.moulberry.notenoughupdates.recipes.CraftingRecipe
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class MinionCraftHelper {

    private val config get() = SkyHanniMod.feature.event.bingo

    private val minionNamePattern by RepoPattern.pattern(
        "bingo.minion.name",
        "(?<name>.*) Minion (?<number>.*)"
    )

    private var display = emptyList<String>()
    private var hasMinionInInventory = false
    private var hasItemsForMinion = false
    private val tierOneMinions = mutableListOf<NEUInternalName>()
    private val tierOneMinionsDone get() = BingoAPI.bingoStorage.tierOneMinionsDone
    private val allIngredients = mutableListOf<NEUInternalName>()
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
        minions: MutableMap<String, NEUInternalName>,
        otherItems: MutableMap<NEUInternalName, Int>,
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

    private fun loadFromInventory(mainInventory: Array<ItemStack?>): Pair<MutableMap<String, NEUInternalName>, MutableMap<NEUInternalName, Int>> {
        init()

        val minions = mutableMapOf<String, NEUInternalName>()
        val otherItems = mutableMapOf<NEUInternalName, Int>()

        for (item in mainInventory) {
            val name = item?.name?.removeColor() ?: continue
            val rawId = item.getInternalName()
            if (isMinionName(name)) {
                minions[name] = rawId
            }
        }

        val allMinions = tierOneMinions.toMutableList()
        minions.values.mapTo(allMinions) { it.addOneToId() }

        for (item in mainInventory) {
            val name = item?.name?.removeColor() ?: continue
            if (item.hasEnchantments()) continue
            val rawId = item.getInternalName()
            if (!isMinionName(name)) {
                if (!allIngredients.contains(rawId)) continue
                if (!isAllowed(allMinions, rawId)) continue

                val (itemId, multiplier) = NEUItems.getMultiplier(rawId)
                val old = otherItems.getOrDefault(itemId, 0)
                otherItems[itemId] = old + item.stackSize * multiplier
            }
        }

        FirstMinionTier.firstMinionTier(otherItems, minions, tierOneMinions, tierOneMinionsDone)
        return Pair(minions, otherItems)
    }

    private fun isAllowed(allMinions: List<NEUInternalName>, internalName: NEUInternalName): Boolean {
        val a = NEUItems.getMultiplier(internalName)
        for (minion in allMinions) {
            val recipes = NEUItems.getRecipes(minion)

            for (recipe in recipes) {
                for (ingredient in recipe.getCachedIngredients()) {
                    val ingredientInternalName = ingredient.internalItemId.asInternalName()
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

        for (internalId in NEUItems.allNeuRepoItems().keys) {
            val internalName = internalId.asInternalName()
            if (internalName.endsWith("_GENERATOR_1")) {
                if (internalName == "REVENANT_GENERATOR_1".asInternalName() ||
                    internalName == "TARANTULA_GENERATOR_1".asInternalName() ||
                    internalName == "VOIDLING_GENERATOR_1".asInternalName() ||
                    internalName == "INFERNO_GENERATOR_1".asInternalName() ||
                    internalName == "VAMPIRE_GENERATOR_1".asInternalName()
                ) continue
                tierOneMinions.add(internalName)
            }

            if (internalName.contains("_GENERATOR_")) {
                for (recipe in NEUItems.getRecipes(internalName)) {
                    if (recipe !is CraftingRecipe) continue

                    for (ingredient in recipe.getCachedIngredients()) {
                        val id = ingredient.internalItemId.asInternalName()
                        if (!id.contains("_GENERATOR_") && !allIngredients.contains(id)) {
                            allIngredients.add(id)
                        }
                    }
                }
            }
        }
    }

    private fun addMinion(
        name: String,
        minionTier: Int,
        minionId: NEUInternalName,
        otherItems: MutableMap<NEUInternalName, Int>,
        newDisplay: MutableList<String>,
    ) {
        val nextTier = minionTier + 1
        val minionName = "§9$name Minion $nextTier"
        newDisplay.add(minionName)
        val nextMinionId = minionId.addOneToId()
        for (recipe in NEUItems.getRecipes(nextMinionId)) {
            if (recipe !is CraftingRecipe) continue
            val output = recipe.output
            val internalItemId = output.internalItemId.asInternalName()
            if (!internalItemId.contains("_GENERATOR_")) continue
            val map = mutableMapOf<NEUInternalName, Int>()
            for (input in recipe.inputs) {
                val itemId = input.internalItemId.asInternalName()
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
                val itemName = rawId.itemName
                val isTool = itemId.startsWith("WOOD_")
                if (percentage >= 1) {
                    val color = if (isTool) "§7" else "§a"
                    newDisplay.add("  $itemName§8: ${color}DONE")
                    otherItems[itemId] = have - needAmount
                } else {
                    if (!config.minionCraftHelperProgressFirst && !isTool && minionId.endsWith("_0")) {
                        newDisplay.removeLast()
                        return
                    }
                    val format = LorenzUtils.formatPercentage(percentage)
                    val haveFormat = have.addSeparators()
                    val needFormat = needAmount.addSeparators()
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

    private fun NEUInternalName.addOneToId(): NEUInternalName {
        val lastText = asString().split("_").last()
        val next = lastText.toInt() + 1
        return replace(lastText, "" + next)
    }

    private fun isMinionName(itemName: String) = itemName.contains(" Minion ") && !itemName.contains(" Minion Skin")

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (event.inventoryName != "Crafted Minions") return

        for ((_, b) in event.inventoryItems) {
            val name = b.name
            if (!name.startsWith("§e")) continue
            val internalName = NEUInternalName.fromItemName("$name I")
                .replace("MINION", "GENERATOR").replace(";", "_").replace("CAVE_SPIDER", "CAVESPIDER")
            tierOneMinionsDone.add(internalName)
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(26, "#player.bingoSessions") { element ->
            for ((_, data) in element.asJsonObject.entrySet()) {
                fixTierOneMinions(data.asJsonObject)
            }
            element
        }
    }

    private fun fixTierOneMinions(data: JsonObject) {
        val uniqueEntries = mutableSetOf<String>()
        val newList = JsonArray()
        var counter = 0
        for (entry in data["tierOneMinionsDone"].asJsonArray) {
            val name = entry.asString
            if (!name.startsWith("INTERNALNAME:") && uniqueEntries.add(name)) {
                newList.add(entry)
            } else {
                counter++
            }
        }
        if (counter > 0) {
            println("Removed $counter wrong entries in fixTierOneMinions.")
        }
        data.add("tierOneMinionsDone", newList)
    }
}
