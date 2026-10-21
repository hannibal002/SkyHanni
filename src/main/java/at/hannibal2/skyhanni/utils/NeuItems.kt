package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.enoughupdates.ItemResolutionQuery
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemAliases
import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemDisplayNamesJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.MultiFilterJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getRepoItemNameFromJson
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.allItemsCache
import at.hannibal2.skyhanni.utils.NeuItems.ambiguousDisplayNames
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.skyhanni.utils.RegexUtils.find
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isVanillaItem
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeNonAsciiNonColorCode
import at.hannibal2.skyhanni.utils.StringUtils.removePrefix
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.compat.getVanillaItem
import at.hannibal2.skyhanni.utils.json.fromJsonOrNull
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonPrimitive
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import java.util.NavigableMap
import java.util.TreeMap
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object NeuItems {
    private val multiplierCache = mutableMapOf<NeuInternalName, PrimitiveItemStack>()
    private val itemIdCache = mutableMapOf<Item, List<NeuInternalName>>()
    private val stackResolutionCache: TimeLimitedCache<NeuInternalName, SafeItemStack> = TimeLimitedCache(2.minutes)
    private val patternGroup = RepoPattern.group("data.neu.items")

    // Internal names excluded from the display name to internal name lookup, because another item uses the same display name.
    private var ignoredDisplayNames = emptySet<NeuInternalName>()

    internal fun isIgnoredDisplayNameItem(internalName: NeuInternalName): Boolean = internalName in ignoredDisplayNames

    /**
     * Display names shared by several items where none of them is the one obviously meant.
     * Resolving them would silently pick a random one, so they resolve to null instead.
     */
    private var ambiguousDisplayNames = emptySet<String>()

    /** Callers without color codes, e.g. chat messages, must be caught as well. */
    private var ambiguousDisplayNamesColorless = emptySet<String>()

    /**
     * WRAPPED-REGEX-TEST: "§7[lvl 1➡100] "
     * WRAPPED-REGEX-TEST: "§7[Lvl {LVL}] "
     * WRAPPED-REGEX-TEST: "§f§f§7[lvl {lvl}] "
     * WRAPPED-REGEX-TEST: "§f§f§7[lvl 1➡100] "
     * WRAPPED-REGEX-TEST: "§f§f§7[Lvl {LVL}] "
     */
    private val neuPetLevelPattern by patternGroup.pattern(
        "pet-level",
        "(?i)(?:§.)+\\[lvl (?:\\d+➡\\d+|\\{lvl})\\] ",
    )

    /**
     * Deliberately looser than [neuPetLevelPattern]: a marker that is still in the name is one
     * that pattern did not match at the start of the name.
     * REGEX-TEST: [lvl 1➡100]
     * REGEX-TEST: [Lvl 1➡200]
     * REGEX-TEST: [lvl {lvl}]
     */
    private val leftoverPetLevelPattern by patternGroup.pattern(
        "pet-level-leftover",
        "(?i)\\[lvl (?:\\d+➡\\d+|\\{lvl})\\]",
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
            SafeItemStack(Blocks.BARRIER).itemType,
            "§cMissing Repo Item",
            "§cYour NEU repo seems to be out of date",
        )
    }

    @HandleEvent
    private suspend fun onRepoReload(event: RepositoryReloadEvent) {
        val ignoredItems = event.getConstant<MultiFilterJson>("IgnoredItems")
        ignoreItemsFilter.load(ignoredItems)
        commonItemAliases = event.getConstant<ItemAliases>("ItemAliases")
        val displayNameData = event.getConstant<ItemDisplayNamesJson>("ItemDisplayNames")
        ignoredDisplayNames = displayNameData.ignoredInternalNames
        ambiguousDisplayNames = displayNameData.ambiguousDisplayNames.mapTo(mutableSetOf()) { normalizeDisplayName(it) }
        ambiguousDisplayNamesColorless = ambiguousDisplayNames.mapTo(mutableSetOf()) { it.removeColor() }

        // The neu repo may have loaded first, in which case the name cache was built without the list above.
        if (allInternalNames.isNotEmpty()) DelayedRun.runOrNextTick(::readAllNeuItems)
    }

    @HandleEvent
    private suspend fun onNeuRepoReload() {
        multiplierCache.clear()
        itemIdCache.clear()
        DelayedRun.runOrNextTick(::readAllNeuItems)
    }

    /** The form display names are stored in, both in [allItemsCache] and in [ambiguousDisplayNames]. */
    internal fun normalizeDisplayName(displayName: String): String =
        displayName.lowercase().removeNonAsciiNonColorCode().trim()

    internal fun isAmbiguousDisplayName(displayName: String): Boolean {
        val name = normalizeDisplayName(displayName)
        return name in ambiguousDisplayNames || name.removeColor() in ambiguousDisplayNamesColorless
    }

    private fun readAllNeuItems() {
        allInternalNames.clear()
        val tempAllItemCache = mutableMapOf<String, NeuInternalName>()
        val tempNoColor = TreeMap<String, NeuInternalName>()
        val duplicates = mutableMapOf<String, MutableList<NeuInternalName>>()
        val nonPetsWithPetSuffix = mutableListOf<NeuInternalName>()
        val leftoverLevelNames = mutableListOf<String>()

        allNeuRepoItems().forEach { (internalName, itemInfo) ->
            allInternalNames[internalName.asString()] = internalName

            // Items sharing their display name with another item, where the other one is the one we want.
            if (internalName in ignoredDisplayNames) return@forEach

            // Every ignored item is named "§cBugged Item", see ItemUtils.getSpecialRepoItemName.
            if (ignoreItemsFilter.match(internalName.asString())) return@forEach

            // The " Pet" suffix is added by ItemUtils.getSpecialRepoItemName, and
            // NeuInternalName.fromItemNameOrNull strips it from every query before the lookup.
            // Stripping it from a non-pet would cache that item under a key no query can reach.
            val isKnownPet = PetUtils.isKnownPetInternalName(internalName)
            val cleanName = internalName.getRepoItemNameFromJson(itemInfo)?.lowercase()?.removePrefix(neuPetLevelPattern)
                ?.let { if (isKnownPet) it.removeSuffix(" pet") else it }?.takeIf { it.isNotEmpty() } ?: run {
                ChatUtils.debug("skipped `$internalName` from readAllNeuItems")
                return@forEach
            }

            if (!isKnownPet && cleanName.endsWith(" pet")) nonPetsWithPetSuffix.add(internalName)

            if (leftoverPetLevelPattern.find(cleanName)) leftoverLevelNames.add("${internalName.asString()}: '$cleanName'")

            val newCleanName = normalizeDisplayName(cleanName)
            if (newCleanName in ambiguousDisplayNames) return@forEach

            tempAllItemCache.put(newCleanName, internalName)?.let { previous ->
                duplicates.getOrPut(newCleanName) { mutableListOf(previous) }.add(internalName)
            }
            tempNoColor[newCleanName.removeColor()] = internalName
        }
        itemNamesWithoutColor = tempNoColor
        allItemsCache = tempAllItemCache
        stackResolutionCache.clear()
        // These resolve through allItemsCache, so they have to follow every rebuild, not just the neu repo event.
        ItemNameResolver.clearCache()
        NeuInternalName.clearItemNameCache()
        ChatUtils.debug("Cleared the NEUItems stack resolution cache")
        reportDuplicateDisplayNames(duplicates)
        reportNonPetsWithPetSuffix(nonPetsWithPetSuffix)
        reportLeftoverLevelNames(leftoverLevelNames)
    }

    /**
     * Which of them wins depends on the iteration order of the neu repo, so it can silently
     * change with a repo update. Anything reported here belongs into ItemDisplayNames.
     */
    private fun reportDuplicateDisplayNames(duplicates: Map<String, List<NeuInternalName>>) {
        if (duplicates.isEmpty()) return
        ChatUtils.debug("Found ${duplicates.size} duplicate item display names, see console for details.")
        for ((displayName, internalNames) in duplicates.toSortedMap()) {
            val all = internalNames.joinToString(", ") { it.asString() }
            println("duplicate item display name '$displayName': $all")
        }
    }

    /**
     * Only known pets are expected to end in " Pet", ItemUtils.getSpecialRepoItemName adds the suffix there.
     * Any other item named this way cannot be resolved through NeuInternalName.fromItemNameOrNull,
     * since that strips the suffix from every query.
     */
    private fun reportNonPetsWithPetSuffix(nonPetsWithPetSuffix: List<NeuInternalName>) {
        if (nonPetsWithPetSuffix.isEmpty()) return
        ErrorManager.logErrorStateWithData(
            "Found non-pet items ending in ' Pet', please report this in discord",
            "Non-pet items ending in ' Pet' cannot be resolved via NeuInternalName.fromItemNameOrNull",
            "internal names" to nonPetsWithPetSuffix.sorted().map { it.asString() },
            betaOnly = true,
        )
    }

    /**
     * A pet level marker [neuPetLevelPattern] did not strip, either because it does not match
     * or because the marker does not sit at the start of the name.
     * [normalizeDisplayName] then drops the non-ASCII arrow, and the lookup side never applies
     * that normalization to a query, so the item is cached under a key nothing can ask for.
     */
    private fun reportLeftoverLevelNames(leftoverLevelNames: List<String>) {
        if (leftoverLevelNames.isEmpty()) return
        ErrorManager.logErrorStateWithData(
            "Found item names with a leftover pet level marker, please report this in discord",
            "Item names still contain a pet level marker that neuPetLevelPattern did not strip",
            "names" to leftoverLevelNames.sorted(),
            betaOnly = true,
        )
    }

    fun getInternalName(itemStack: SafeItemStack): NeuInternalName? = ItemResolutionQuery()
        .withCurrentGuiContext()
        .withItemStack(itemStack)
        .resolveInternalName()

    fun getInternalNameFromHypixelIdOrNull(hypixelId: String): NeuInternalName? {
        val internalName = hypixelId.replace(':', '-')
        return internalName.toInternalName().takeIf { it.getItemStackOrNull() != null }
    }

    fun transHypixelNameToInternalName(hypixelId: String): NeuInternalName =
        ItemResolutionQuery.transformHypixelBazaarToNeuItemId(hypixelId).toInternalName()

    fun NeuInternalName.getItemStackOrNull(): SafeItemStack? = stackResolutionCache.getOrPut(this) {
        ItemResolutionQuery().withKnownInternalName(this).resolveToItemStack()
            ?: return null
    }.copy()

    fun NeuInternalName.getItemStack(): SafeItemStack =
        getItemStackOrNull() ?: run {
            getPriceOrNull() ?: return@run fallbackItem
            if (ignoreItemsFilter.match(this.asString())) return@run fallbackItem

            val name = this.toString()
            ItemUtils.addMissingRepoItem(name, "Could not create item stack for $name")
            fallbackItem
        }

    fun isVanillaItem(item: SafeItemStack): Boolean = item.getInternalName().isVanillaItem()

    // todo repo
    private val hardcodedVanillaItems = listOf(
        "WOOD_AXE", "WOOD_HOE", "WOOD_PICKAXE", "WOOD_SPADE", "WOOD_SWORD",
        "GOLD_AXE", "GOLD_HOE", "GOLD_PICKAXE", "GOLD_SPADE", "GOLD_SWORD",
    )

    fun NeuInternalName.isVanillaItem(): Boolean {
        val asString = this.asString()
        if (hardcodedVanillaItems.contains(asString)) return true

        val vanillaName = asString.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        val internalizedVanillaName = vanillaName.toInternalName()
        if (allNeuRepoItems().containsKey(internalizedVanillaName)) {
            val itemJson = allNeuRepoItems()[internalizedVanillaName]
            if (itemJson != null && itemJson.vanilla) return true
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

    fun allNeuRepoInternalNames(): Set<NeuInternalName> = EnoughUpdatesManager.getInternalNames()
    fun allNeuRepoItems(): Map<NeuInternalName, NeuItemJson> = EnoughUpdatesManager.getItemInformation()

    fun getInternalNamesForItemId(item: Item): List<NeuInternalName> {
        itemIdCache[item]?.let {
            return it
        }
        val result = allNeuRepoItems().filter {
            it.value.itemId.getVanillaItem() == item
        }.keys.toList()
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
            for (ingredient in recipe.ingredients) {
                addRecipeIngredient(ingredient, internalName, map)
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

    private fun addRecipeIngredient(
        ingredient: PrimitiveIngredient,
        resultInternalName: NeuInternalName,
        map: MutableMap<NeuInternalName, Int>,
    ) {
        var internalItemId = ingredient.internalName
        // ignore cactus green
        if (resultInternalName == "ENCHANTED_CACTUS_GREEN".toInternalName() && internalItemId == "INK_SACK-2".toInternalName()) {
            internalItemId = "CACTUS".toInternalName()
        }

        // ignore rabbit hide in leather
        if (resultInternalName == "LEATHER".toInternalName() && internalItemId == "RABBIT_HIDE".toInternalName()) {
            return
        }

        map.addOrPut(internalItemId, ingredient.count.toInt())
    }

    fun getRecipes(internalName: NeuInternalName): Set<PrimitiveRecipe> = EnoughUpdatesManager.getRecipesFor(internalName)

    fun saveNBTData(item: SafeItemStack, removeLore: Boolean = true): String {
        val jsonObject = EnoughUpdatesManager.stackToJson(item)
        if (!jsonObject.has("internalname")) {
            jsonObject.add("internalname", JsonPrimitive("_"))
        }
        if (removeLore && jsonObject.has("lore")) jsonObject.remove("lore")
        val jsonString = jsonObject.toString()
        return StringUtils.encodeBase64(jsonString)
    }

    fun loadNBTData(encoded: String): SafeItemStack {
        val jsonString = StringUtils.decodeBase64(encoded)
        val neuItem = ConfigManager.gson.fromJsonOrNull<NeuItemJson>(jsonString) ?: run {
            ErrorManager.logErrorStateWithData(
                "Could not parse NEU item from encoded string",
                internalMessage = "Could not load NEU item from encoded string - GSON parsing failed",
                "encoded" to encoded,
                "jsonString" to jsonString,
            )
            return ItemUtils.createItemStack(Items.MAP, "unloaded")
        }
        return EnoughUpdatesManager.neuItemToStack(neuItem, useCache = false)
    }
}
