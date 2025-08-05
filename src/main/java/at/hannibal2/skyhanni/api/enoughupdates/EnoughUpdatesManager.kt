package at.hannibal2.skyhanni.api.enoughupdates

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NEURaritySpecificPetNums
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetNumsJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetsJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuAbstractRecipe
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.setLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetInfo
import at.hannibal2.skyhanni.utils.StringUtils.cleanString
import at.hannibal2.skyhanni.utils.StringUtils.removeUnusedDecimal
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.mapNotNullAsync
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.skyhanni.utils.compat.getIdentifierString
import at.hannibal2.skyhanni.utils.compat.getVanillaItem
import at.hannibal2.skyhanni.utils.compat.setCustomItemName
import at.hannibal2.skyhanni.utils.json.fromJsonOrNull
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import java.io.File
import java.util.TreeMap
import kotlin.collections.set
import kotlin.math.floor

//#if MC > 1.21
//$$ import net.minecraft.registry.Registries
//$$ import net.minecraft.util.Identifier
//$$ import net.minecraft.nbt.NbtString
//$$ import net.minecraft.text.Text
//$$ import net.minecraft.component.DataComponentTypes
//$$ import net.minecraft.component.type.LoreComponent
//$$ import at.hannibal2.skyhanni.utils.ComponentUtils
//$$ import at.hannibal2.skyhanni.utils.ItemUtils.setLore
//#endif

// Most functions are taken from NotEnoughUpdates
@SkyHanniModule
object EnoughUpdatesManager {

    val configDirectory = File("config/notenoughupdates")
    private val repoDirectory = File(configDirectory, "repo")
    private val itemsFolder = File(repoDirectory, "items")

    private val loadingMutex = Mutex()
    private val itemMap = TreeMap<NeuInternalName, NeuItemJson>()
    private val internalNameSet: MutableSet<NeuInternalName> = mutableSetOf()
    private val itemStackCache = mutableMapOf<NeuInternalName, ItemStack>()
    private val displayNameCache = mutableMapOf<NeuInternalName, String>()
    private val recipesMap = HashMap<NeuInternalName, MutableSet<PrimitiveRecipe>>()

    private var neuPetsJson: NeuPetsJson? = null
    private var neuPetNums: NeuPetNumsJson? = null

    val titleWordMap = TreeMap<String, MutableMap<String, MutableList<Int>>>()

    fun getInternalNames() = internalNameSet
    fun getItemInformation() = itemMap

    fun inLoadingState() = loadingMutex.isLocked || EnoughUpdatesRepoManager.repoMutex.isLocked

    /**
     * Called by the Neu Repo Manager when the NEU repo is reloaded.
     */
    suspend fun reloadItemsFromRepo(): Unit = loadingMutex.withLock {
        itemStackCache.clear()
        displayNameCache.clear()
        itemMap.clear()
        internalNameSet.clear()
        titleWordMap.clear()
        recipesMap.clear()

        val tempItemMap = TreeMap<NeuInternalName, NeuItemJson>()
        loadItemMap(tempItemMap)

        synchronized(itemMap) {
            itemMap.putAll(tempItemMap)
        }

        synchronized(internalNameSet) {
            internalNameSet.addAll(itemMap.keys)
        }
    }

    fun getRecipesFor(internalName: NeuInternalName): Set<PrimitiveRecipe> = recipesMap.getOrDefault(internalName, emptySet())

    private suspend fun loadItemMap(tempItemMap: TreeMap<NeuInternalName, NeuItemJson>) = coroutineScope {
        val fileSystem = EnoughUpdatesRepoManager.repoFileSystem
        fileSystem.list("items").mapNotNullAsync { name ->
            try {
                val internalName = name.removeSuffix(".json")
                val itemJson = fileSystem.readAllBytesAsJsonElement("items/$name").asJsonObject
                val parsed = parseItem(
                    internalName = internalName,
                    json = itemJson,
                ) ?: return@mapNotNullAsync null
                internalName.toInternalName() to parsed
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(e, "Failed to parse item: $name")
                null
            }
        }.forEach { (internalName, item) ->
            tempItemMap[internalName] = item
        }
    }

    private fun NeuAbstractRecipe.loadAndRegister(itemJson: NeuItemJson) {
        val ingredients = this.getPrimitiveInputs(itemJson).toSet()
        val outputs = this.getPrimitiveOutputs(itemJson).toSet()
        val recipe = PrimitiveRecipe(
            ingredients,
            outputs,
            recipeType = this.type,
            shouldUseForCraftCost = this.type.useForCraftCost,
        )
        for (internalName in recipe.outputs) {
            val recipeSet = recipesMap.getOrPut(internalName.internalName) { mutableSetOf() }
            recipeSet.add(recipe)
        }
    }

    private fun parseItem(internalName: String, json: JsonObject): NeuItemJson? = runCatching {
        val itemJson: NeuItemJson = ConfigManager.gson.fromJsonOrNull<NeuItemJson>(json) ?: return@runCatching null
        // If the itemId is vanilla, replace it with the vanilla item identifier
        itemJson.itemId.getVanillaItem()?.let { mcItem ->
            itemJson.itemId = mcItem.getIdentifierString()
        }
        // Crafting type recipe
        itemJson.recipe?.loadAndRegister(itemJson)
        // Other types of recipes
        itemJson.recipes.forEach { recipe ->
            recipe.loadAndRegister(itemJson)
        }
        itemJson.displayName?.let { displayName ->
            synchronized(titleWordMap) {
                for ((index, str) in displayName.split(" ").withIndex()) {
                    val cleanedStr = str.cleanString()
                    val internalMap = titleWordMap.getOrPut(cleanedStr) { TreeMap() }
                    val indexList = internalMap.getOrPut(internalName) { mutableListOf() }
                    indexList.add(index)
                }
            }
        }
        return itemJson
    }.getOrNull()

    fun getItemById(id: String): NeuItemJson? = itemMap[id.toInternalName()]
    fun getItemById(internalName: NeuInternalName): NeuItemJson? = itemMap[internalName]

    fun stackToJson(stack: ItemStack): JsonObject {
        val tag = stack.tagCompound ?: NBTTagCompound()

        val lore = stack.getLore()

        val json = JsonObject()
        json.addProperty("itemid", stack.item.getIdentifierString())
        json.addProperty("displayname", stack.displayName)
        //#if MC < 1.21
        json.addProperty("nbttag", tag.toString())
        json.addProperty("damage", stack.itemDamage)
        //#else
        //$$ json.add("nbttag", ComponentUtils.convertToNeuNbtInfoJson(stack))
        //#endif

        val jsonLore = JsonArray()
        for (line in lore) {
            jsonLore.add(JsonPrimitive(line))
        }
        json.add("lore", jsonLore)
        return json
    }

    fun neuItemToStack(neuItem: NeuItemJson, useCache: Boolean = true, useReplacements: Boolean = false): ItemStack =
        neuItem.toStack(useCache, useReplacements)

    private fun NeuItemJson?.toStack(
        useCache: Boolean = true,
        useReplacements: Boolean = false
    ): ItemStack {
        //#if MC < 1.21
        this ?: return ItemStack(Items.painting)
        //#else
        //$$ this ?: return ItemStack(Items.PAINTING)
        //#endif

        var usingCache = useCache && !useReplacements
        if (internalName.asString() == "_") usingCache = false
        if (usingCache) itemStackCache[internalName]?.let { return it.copy() }

        //#if MC < 1.21
        val defaultStack = ItemStack(Item.getItemFromBlock(Blocks.stone), 0, 255)
        val baseItem = itemId.getVanillaItem() ?: return defaultStack
        //#else
        //$$ val lDamage = this.damage ?: 0
        //$$ val defaultStack = ItemStack(Blocks.STONE.asItem())
        //$$ val baseItem = ComponentUtils.convertMinecraftIdToModern(itemId, lDamage).getVanillaItem() ?: run {
        //$$     ErrorManager.skyHanniError("Invalid itemId: $itemId with damage $lDamage")
        //$$     return defaultStack
        //$$ }
        //#endif

        val stack = ItemStack(baseItem).takeIf { it.isNotEmpty() } ?: return defaultStack

        count?.let { stack.stackSize = it }
        //#if MC < 1.21
        damage?.let { stack.itemDamage = it }
        try {
            stack.tagCompound = this.nbtTag
        } catch (_: Error) {}
        //#else
        //$$ ComponentUtils.convertToComponents(stack, neuNbt)
        //#endif

        var replacements = mapOf<String, String>()
        if (useReplacements) {
            replacements = stack.getPetLoreReplacements()
            displayName?.let {
                var name = it
                for ((key, value) in replacements) {
                    name = name.replace("{$key}", value)
                }
                stack.setCustomItemName(name)
            }
        }

        lore.takeIfNotEmpty()?.let { stack.setLore(processLore(lore, replacements)) }

        if (usingCache) itemStackCache[internalName] = stack
        return stack.copy()
    }

    private fun ItemStack?.getPetLoreReplacements(): Map<String, String> {
        val petInfo = this?.getPetInfo() ?: return emptyMap()
        val properInternalName = petInfo.type
        // We let PetData do the heavy lifting of parsing the pet info
        val petData = PetData(petInfo)
        return buildMap {
            put("LVL", petData.getLevelReplacement(properInternalName))
            val raritySpecific = neuPetNums?.get(properInternalName)?.get(petData.rarity) ?: return@buildMap
            if (petData.level == 0) addUnlevelledStatReplacements(raritySpecific)
            else addLevelledStatReplacements(petData, raritySpecific)
        }
    }

    private fun PetData.getLevelReplacement(properInternalName: String): String {
        return if (level != 0) level.toString()
        else neuPetsJson?.customPetLeveling?.get(properInternalName)?.let { petLeveling ->
            val maxLevel = petLeveling.maxLevel ?: 100
            "1➡$maxLevel"
        } ?: "1➡100"
    }

    /**
     * Displays stat ranges for pets in the case there is not a static level to check stats of.
     */
    private fun MutableMap<String, String>.addUnlevelledStatReplacements(nums: NEURaritySpecificPetNums) {
        val otherNumsMin = nums.min.otherNums
        val otherNumsMax = nums.max.otherNums

        val addZero = nums.statLevellingType == 1

        for (i in 0..otherNumsMax.size) {
            val start = if (addZero) "0➡" else ""
            this[i.toString()] = "$start${otherNumsMin[i]}➡${otherNumsMax[i]}"
        }

        for ((stat, statMax) in nums.max.statNums) {
            val statMin = nums.min.statNums[stat] ?: continue
            val start = "${if (addZero) "0➡" else ""}${if (statMin > 0) "+" else ""}"
            this[stat.name] = "$start$statMin➡$statMax"
        }
    }

    /**
     * Adds 'true' calculated stats based on pet level.
     */
    private fun MutableMap<String, String>.addLevelledStatReplacements(petData: PetData, nums: NEURaritySpecificPetNums) {
        val level = petData.level
        val otherNumsMin = nums.min.otherNums
        val otherNumsMax = nums.max.otherNums

        val minStatLevel = nums.minStatsLevel ?: 0
        val maxStatLevel = nums.maxStatsLevel ?: 100
        val statLevellingType = nums.statLevellingType ?: -1
        val statsLevel = if (statLevellingType in 0..1) {
            if (level < minStatLevel) 1
            else if (level < maxStatLevel) level - minStatLevel + 1
            else maxStatLevel - minStatLevel + 1
        } else level

        val minMix = (maxStatLevel - (minStatLevel - (if (statLevellingType == -1) 0 else 1)) - statsLevel) / 99f
        val maxMix = (statsLevel - 1) / 99f
        for (i in otherNumsMax.indices) {
            val num = otherNumsMin[i] * minMix + otherNumsMax[i] * maxMix
            this[i.toString()] = if (statLevellingType == 1 && level < minStatLevel) "0"
            else (floor(num * 10) / 10f).removeUnusedDecimal()
        }

        for ((stat, statMax) in nums.max.statNums) {
            this[stat.name] = if (statLevellingType == 1 && level < minStatLevel) "0"
            else {
                val statMin = nums.min.statNums[stat] ?: continue
                val num = statMin * minMix + statMax * maxMix
                val baseFormat = (if (statMin > 0) "+" else "")
                baseFormat + (floor(num * 10) / 10).removeUnusedDecimal()
            }
        }
    }

    private fun processLore(lore: List<String>, replacements: Map<String, String>): List<String> = buildList {
        lore.onEach { line ->
            var replacedLine = line
            for ((key, value) in replacements) {
                replacedLine = replacedLine.replace("{$key}", value)
            }
            add(replacedLine)
        }
    }

    fun getDisplayName(internalName: NeuInternalName): String = displayNameCache.getOrPut(internalName) {
        // Intentionally toString() instead of asString() to indicate failure
        val itemInfo = getItemById(internalName) ?: return@getOrPut internalName.toString()
        itemInfo.displayName ?: run {
            ErrorManager.skyHanniError("No displayname for $internalName")
        }
    }

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        neuPetsJson = event.getConstant<NeuPetsJson>("pets")
        neuPetNums = event.getConstant<NeuPetNumsJson>("petnums")
        if (itemMap.isNotEmpty()) {
            ChatUtils.chat("Reloaded ${itemMap.size.addSeparators()} items in the NEU repo")
        }
    }

    fun reportItemStatus() {
        val loadedItems = itemMap.size
        val directorySize = itemsFolder.listFiles()?.size ?: 0

        val status = when {
            directorySize == 0 -> "§cNo items directory found!"
            loadedItems == 0 -> "§cNo items loaded!"
            loadedItems < directorySize -> "§eLoaded $loadedItems/$directorySize items"
            loadedItems > directorySize -> "§eLoaded Items: $loadedItems (more than directory size)"
            else -> "§aLoaded all $loadedItems items!"
        }
        ChatUtils.chat("  §aNEU Repo Item Status:\n  $status", prefix = false)
    }
}
