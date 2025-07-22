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
import at.hannibal2.skyhanni.utils.StringUtils.removeNonAsciiNonColorCode
import at.hannibal2.skyhanni.utils.StringUtils.removePrefix
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.compat.getVanillaItem
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
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
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object NeuItems {
    private val multiplierCache = mutableMapOf<NeuInternalName, PrimitiveItemStack>()
    private val itemIdCache = mutableMapOf<Item, List<NeuInternalName>>()
    private val stackResolutionCache: TimeLimitedCache<NeuInternalName, ItemStack> = TimeLimitedCache(2.minutes)
    private val patternGroup = RepoPattern.group("data.neu.items")

    /**
     * WRAPPED-REGEX-TEST: "§7[lvl 1➡100] "
     * WRAPPED-REGEX-TEST: "§f§f§7[lvl {lvl}] "
     * WRAPPED-REGEX-TEST: "§f§f§7[lvl 1➡100] "
     * WRAPPED-REGEX-TEST: "§f§f§7[Lvl {LVL}] "
     */
    private val neuPetLevelRegex by patternGroup.pattern(
        "pet-level",
        "(?i)(?:§.)+\\[lvl (?:\\d+➡\\d+|\\{lvl})\\] "
    )

    /** Keys are internal names as String */
    val allInternalNames: NavigableMap<String, NeuInternalName> = TreeMap()
    val ignoreItemsFilter = MultiFilter()

    private var itemNamesWithoutColor: NavigableMap<String, NeuInternalName> = TreeMap()

    var commonItemAliases: ItemAliases = ItemAliases()
        private set

    var allItemsCache = mapOf<String, NeuInternalName>() // item name -> internal name
        private set

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
        commonItemAliases = event.getConstant<ItemAliases>("ItemAliases")
    }

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        DelayedRun.onThread.execute {
            readAllNeuItems()
        }
    }

    private fun readAllNeuItems() {
        allInternalNames.clear()
        val tempAllItemCache = mutableMapOf<String, NeuInternalName>()
        val tempNoColor = TreeMap<String, NeuInternalName>()

        allNeuRepoItems().keys.forEach { rawInternalName ->
            // we ignore all builder blocks from the item name -> internal name cache
            // because builder blocks can have the same display name as normal items.
            if (rawInternalName.startsWith("BUILDER_")) return@forEach

            val internalName = rawInternalName.toInternalName()
            val stack = internalName.getItemStackOrNull() ?: run {
                ChatUtils.debug("skipped `$this`from readAllNeuItems")
                return@forEach
            }
            val cleanName = stack.displayName?.lowercase()?.removePrefix(neuPetLevelRegex)?.takeIf {
                it.isNotEmpty()
            } ?: return@forEach

            if (cleanName.contains("[lvl 1➡100]")) {
                if (PlatformUtils.isDevEnvironment) error("wrong name: '$cleanName'")
                else println("wrong name: '$cleanName'")
            }

            val newCleanName = cleanName.removeNonAsciiNonColorCode().trim()

            tempAllItemCache[newCleanName] = internalName
            tempNoColor[newCleanName.removeColor()] = internalName
            allInternalNames[rawInternalName] = internalName
        }
        @Suppress("UNCHECKED_CAST")
        itemNamesWithoutColor = tempNoColor as NavigableMap<String, NeuInternalName>
        allItemsCache = tempAllItemCache
        stackResolutionCache.clear()
        ChatUtils.debug("Cleared the NEUItems stack resolution cache")
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

    fun NeuInternalName.getItemStackOrNull(): ItemStack? = stackResolutionCache.getOrPut(this) {
        ItemResolutionQuery().withKnownInternalName(asString()).resolveToItemStack()
            ?: return null
    }.copy()

    fun NeuInternalName.getItemStack(): ItemStack =
        getItemStackOrNull() ?: run {
            getPriceOrNull() ?: return@run fallbackItem
            if (ignoreItemsFilter.match(this.asString())) return@run fallbackItem

            val name = this.toString()
            ItemUtils.addMissingRepoItem(name, "Could not create item stack for $name")
            fallbackItem
        }

    fun isVanillaItem(item: ItemStack): Boolean = item.getInternalName().isVanillaItem()

    // todo repo
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
        //#if MC < 1.16
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
