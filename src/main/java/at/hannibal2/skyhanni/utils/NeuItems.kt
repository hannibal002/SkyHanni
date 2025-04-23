package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.enoughupdates.ItemResolutionQuery
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemAliases
import at.hannibal2.skyhanni.data.jsonobjects.repo.MultiFilterJson
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.PrimitiveIngredient.Companion.toPrimitiveItemStacks
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isVanillaItem
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeNonAscii
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.compat.getVanillaItem
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import io.github.moulberry.notenoughupdates.NEUOverlay
import io.github.moulberry.notenoughupdates.overlays.AuctionSearchOverlay
import io.github.moulberry.notenoughupdates.overlays.BazaarSearchOverlay
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import java.util.NavigableMap
import java.util.TreeMap

@SkyHanniModule
object NeuItems {
    private val multiplierCache = mutableMapOf<NeuInternalName, PrimitiveItemStack>()
    private val itemIdCache = mutableMapOf<Item, List<NeuInternalName>>()

    var allItemsCache = mapOf<String, NeuInternalName>() // item name -> internal name
    var itemNamesWithoutColor: NavigableMap<String, NeuInternalName> = TreeMap()

    /** Keys are internal names as String */
    val allInternalNames: NavigableMap<String, NeuInternalName> = TreeMap()
    val ignoreItemsFilter = MultiFilter()

    var commonItemAliases: ItemAliases = ItemAliases()

    private val fallbackItem by lazy {
        ItemUtils.createItemStack(
            ItemStack(Blocks.barrier).item,
            "§cMissing Repo Item",
            "§cYour NEU repo seems to be out of date",
        )
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val ignoredItems = event.getConstant<MultiFilterJson>("IgnoredItems")
        ignoreItemsFilter.load(ignoredItems)
        val aliases = event.getConstant<ItemAliases>("ItemAliases")
        commonItemAliases = aliases
    }

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        readAllNeuItems()
    }

    private fun readAllNeuItems() {
        allInternalNames.clear()
        val map = mutableMapOf<String, NeuInternalName>()
        val noColor = TreeMap<String, NeuInternalName>()
        for (rawInternalName in allNeuRepoItems().keys) {
            val internalName = rawInternalName.toInternalName()
            var name = internalName.getItemStackOrNull()?.displayName?.lowercase() ?: run {
                ChatUtils.debug("skipped `$rawInternalName` from readAllNeuItems")
                continue
            }

            // we ignore all builder blocks from the item name -> internal name cache
            // because builder blocks can have the same display name as normal items.
            if (rawInternalName.startsWith("BUILDER_")) continue

            // TODO remove all except one of them once neu is consistent
            name = name.removePrefix("§f§f§7[lvl 1➡100] ")
            name = name.removePrefix("§f§f§7[lvl {lvl}] ")
            name = name.removePrefix("§7[lvl 1➡100] ")

            if (name.contains("[lvl 1➡100]")) {
                if (PlatformUtils.isDevEnvironment) {
                    error("wrong name: '$name'")
                }
                println("wrong name: '$name'")
            }

            name.removeNonAscii().trim()

            map[name] = internalName
            noColor[name.removeColor()] = internalName
            allInternalNames[rawInternalName] = internalName
        }
        @Suppress("UNCHECKED_CAST")
        itemNamesWithoutColor = noColor as NavigableMap<String, NeuInternalName>
        allItemsCache = map
    }

    fun getInternalName(itemStack: ItemStack): String? = ItemResolutionQuery()
        .withCurrentGuiContext()
        .withItemStack(itemStack)
        .resolveInternalName()

    fun getInternalNameFromHypixelIdOrNull(hypixelId: String): NeuInternalName? {
        val internalName = hypixelId.replace(':', '-')
        return internalName.toInternalName().takeIf { it.getItemStackOrNull() != null }
    }

    fun getInternalNameFromHypixelId(hypixelId: String): NeuInternalName =
        getInternalNameFromHypixelIdOrNull(hypixelId)
            ?: error("hypixel item id does not match internal name: $hypixelId")

    fun transHypixelNameToInternalName(hypixelId: String): NeuInternalName =
        ItemResolutionQuery.transformHypixelBazaarToNeuItemId(hypixelId).toInternalName()

    //  TODO add cache
    fun NeuInternalName.getItemStackOrNull(): ItemStack? = ItemResolutionQuery()
        .withKnownInternalName(asString())
        .resolveToItemStack()?.copy()

    fun getItemStackOrNull(internalName: String) = internalName.toInternalName().getItemStackOrNull()

    fun NeuInternalName.getItemStack(): ItemStack =
        getItemStackOrNull() ?: run {
            getPriceOrNull() ?: return@run fallbackItem
            if (ignoreItemsFilter.match(this.asString())) return@run fallbackItem

            val name = this.toString()
            ItemUtils.addMissingRepoItem(name, "Could not create item stack for $name")
            fallbackItem
        }

    fun isVanillaItem(item: ItemStack): Boolean = item.getInternalName().isVanillaItem()

    private val hardcodedVanillaItems = listOf(
        "WOOD_AXE", "WOOD_HOE", "WOOD_PICKAXE", "WOOD_SPADE", "WOOD_SWORD",
        "GOLD_AXE", "GOLD_HOE", "GOLD_PICKAXE", "GOLD_SPADE", "GOLD_SWORD",
    )

    fun NeuInternalName.isVanillaItem(): Boolean {
        val asString = this.asString()
        if (hardcodedVanillaItems.contains(asString)) return true

        val vanillaName = asString.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        if (allNeuRepoItems().containsKey(vanillaName)) {
            val json = allNeuRepoItems()[vanillaName]
            if (json != null && json.has("vanilla") && json["vanilla"].asBoolean) return true
        }
        return isVanillaItem(vanillaName)
    }

    private val generatorPattern = "GENERATOR_\\d+".toPattern()

    fun NeuInternalName.isGenerator(): Boolean = generatorPattern.matches(this.asString())

    fun NeuInternalName.removePrefix(prefix: String): NeuInternalName {
        if (prefix.isEmpty()) return this
        val string = asString()
        if (!string.startsWith(prefix)) return this
        return string.substring(prefix.length).toInternalName()
    }

    const val ITEM_FONT_SIZE = 2.0 / 3.0

    fun allNeuRepoItems(): Map<String, JsonObject> = EnoughUpdatesManager.getItemInformation()

    fun getInternalNamesForItemId(item: Item): List<NeuInternalName> {
        itemIdCache[item]?.let {
            return it
        }
        val result = allNeuRepoItems().filter {
            it.value["itemid"].asString.getVanillaItem() == item
        }.keys.map {
            it.toInternalName()
        }
        itemIdCache[item] = result
        return result
    }

    fun findInternalNameStartingWithWithoutNPCs(prefix: String, valid: (NeuInternalName) -> Boolean): Set<String> =
        StringUtils.subMapOfStringsStartingWith(prefix, allInternalNames).filterNot { npcInternal.matches(it.key) }
            .filter { valid(it.value) }.keys

    private val npcName = ".*\\((?:(?:rift )?npc|monster|mayor)\\)".toPattern()
    private val npcInternal = ".*\\((?:(?:RIFT_)?NPC|MONSTER|MAYOR)\\)".toPattern()

    fun findItemNameStartingWithWithoutNPCs(prefix: String, valid: (NeuInternalName) -> Boolean): Set<String> =
        findItemNameStartingWith(prefix).filterNot { npcName.matches(it.key) }.filter { valid(it.value) }.keys

    fun findItemNameStartingWith(prefix: String) = StringUtils.subMapOfStringsStartingWith(prefix, itemNamesWithoutColor)

    fun getPrimitiveMultiplier(internalName: NeuInternalName, tryCount: Int = 0): PrimitiveItemStack {
        multiplierCache[internalName]?.let { return it }
        if (tryCount == 10) {
            ErrorManager.logErrorStateWithData(
                "Could not load recipe data.",
                "Failed to find item multiplier",
                "internalName" to internalName,
            )
            return internalName.makePrimitiveStack()
        }
        for (recipe in getRecipes(internalName)) {
            if (!recipe.isCraftingRecipe()) continue

            val map = mutableMapOf<NeuInternalName, Int>()
            for (ingredient in recipe.ingredients.toPrimitiveItemStacks()) {
                val amount = ingredient.amount
                var internalItemId = ingredient.internalName
                // ignore cactus green
                if (internalName == "ENCHANTED_CACTUS_GREEN".toInternalName() && internalItemId == "INK_SACK-2".toInternalName()) {
                    internalItemId = "CACTUS".toInternalName()
                }

                // ignore wheat in enchanted cookie
                if (internalName == "ENCHANTED_COOKIE".toInternalName() && internalItemId == "WHEAT".toInternalName()) {
                    continue
                }

                // ignore golden carrot in enchanted golden carrot
                if (internalName == "ENCHANTED_GOLDEN_CARROT".toInternalName() && internalItemId == "GOLDEN_CARROT".toInternalName()) {
                    continue
                }

                // ignore rabbit hide in leather
                if (internalName == "LEATHER".toInternalName() && internalItemId == "RABBIT_HIDE".toInternalName()) {
                    continue
                }

                map.addOrPut(internalItemId, amount)
            }
            if (map.size != 1) continue
            val current = map.iterator().next().toPair()
            val id = current.first
            return if (current.second > 1) {
                val child = getPrimitiveMultiplier(id, tryCount + 1)
                val result = child * current.second
                multiplierCache[internalName] = result
                result
            } else {
                internalName.makePrimitiveStack()
            }
        }

        val result = internalName.makePrimitiveStack()
        multiplierCache[internalName] = result
        return result
    }

    fun getRecipes(internalName: NeuInternalName): Set<PrimitiveRecipe> = EnoughUpdatesManager.getRecipesFor(internalName)

    fun neuHasFocus(): Boolean {
        //#if MC < 1.12
        if (!PlatformUtils.isNeuLoaded()) return false
        if (AuctionSearchOverlay.shouldReplace()) return true
        if (BazaarSearchOverlay.shouldReplace()) return true
        // TODO add RecipeSearchOverlay via RecalculatingValue and reflection
        // https://github.com/NotEnoughUpdates/NotEnoughUpdates/blob/master/src/main/java/io/github/moulberry/notenoughupdates/overlays/RecipeSearchOverlay.java
        if (InventoryUtils.inStorage() && InventoryUtils.isNeuStorageEnabled) return true
        if (NEUOverlay.searchBarHasFocus) return true
        //#endif
        return false
    }

    fun saveNBTData(item: ItemStack, removeLore: Boolean = true): String {
        val jsonObject = EnoughUpdatesManager.stackToJson(item)
        if (!jsonObject.has("internalname")) {
            jsonObject.add("internalname", JsonPrimitive("_"))
        }
        if (removeLore && jsonObject.has("lore")) jsonObject.remove("lore")
        val jsonString = jsonObject.toString()
        return StringUtils.encodeBase64(jsonString)
    }

    fun loadNBTData(encoded: String): ItemStack {
        val jsonString = StringUtils.decodeBase64(encoded)
        val jsonObject = ConfigManager.gson.fromJson(jsonString, JsonObject::class.java)
        return EnoughUpdatesManager.jsonToStack(jsonObject, false)
    }
}
