package at.hannibal2.skyhanni.api.enoughupdates

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.other.NeuNbtInfoJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetsJson
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.extraAttributes
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import at.hannibal2.skyhanni.utils.StringUtils.cleanString
import at.hannibal2.skyhanni.utils.StringUtils.removeUnusedDecimal
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.mapNotNullAsync
import at.hannibal2.skyhanni.utils.compat.getIdentifierString
import at.hannibal2.skyhanni.utils.compat.getVanillaItem
import at.hannibal2.skyhanni.utils.compat.setCustomItemName
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
import net.minecraft.nbt.JsonToNBT
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import java.io.File
import java.util.TreeMap
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
//#else
import net.minecraft.nbt.NBTTagString
//#endif

// Most functions are taken from NotEnoughUpdates
@SkyHanniModule
object EnoughUpdatesManager {

    val configDirectory = File("config/notenoughupdates")
    val repoDirectory = File(configDirectory, "repo")

    private val loadingMutex = Mutex()
    private val itemMap = TreeMap<String, JsonObject>()
    private val itemStackCache = mutableMapOf<String, ItemStack>()
    private val displayNameCache = mutableMapOf<String, String>()
    private val recipesMap = mutableMapOf<NeuInternalName, MutableSet<PrimitiveRecipe>>()

    private var neuPetsJson: NeuPetsJson? = null
    private var neuPetNums: JsonObject? = null

    val titleWordMap = TreeMap<String, MutableMap<String, MutableList<Int>>>()

    fun getItemInformation() = itemMap

    fun inLoadingState() = loadingMutex.isLocked || EnoughUpdatesRepoManager.repoMutex.isLocked

    /**
     * Called by the Neu Repo Manager when the NEU repo is reloaded.
     */
    suspend fun reloadItemsFromRepo() = loadingMutex.withLock {
        itemStackCache.clear()
        displayNameCache.clear()
        itemMap.clear()
        titleWordMap.clear()
        recipesMap.clear()

        val tempItemMap = TreeMap<String, JsonObject>()
        loadItemMap(tempItemMap)

        synchronized(itemMap) {
            itemMap.clear()
            itemMap.putAll(tempItemMap)
        }
    }

    fun getRecipesFor(internalName: NeuInternalName): Set<PrimitiveRecipe> = recipesMap.getOrDefault(internalName, emptySet())

    private suspend fun loadItemMap(tempItemMap: TreeMap<String, JsonObject>) = coroutineScope {
        val fileSystem = EnoughUpdatesRepoManager.repoFileSystem
        fileSystem.list("items").mapNotNullAsync { name ->
            val internalName = name.removeSuffix(".json")
            val parsed = parseItem(
                internalName = internalName,
                json = fileSystem.readAllBytesAsJsonElement("items/$name").asJsonObject,
            ) ?: return@mapNotNullAsync null
            internalName to parsed
        }.forEach { (internalName, item) ->
            tempItemMap[internalName] = item
        }
    }

    private fun parseItem(internalName: String, json: JsonObject): JsonObject? {
        if (json.get("itemid") == null) return null
        var itemId = json["itemid"].asString
        val mcItem = itemId.getVanillaItem()
        if (mcItem != null) {
            itemId = mcItem.getIdentifierString()
        }
        json.addProperty("itemid", itemId)

        json["recipe"]?.asJsonObject?.let { recipe ->
            PrimitiveRecipe.loadRecipeFromJson(recipe.asJsonObject, json)
        }
        json["recipes"]?.asJsonArray?.forEach { recipe ->
            PrimitiveRecipe.loadRecipeFromJson(recipe.asJsonObject, json)
        }

        if (json.has("displayname")) {
            synchronized(titleWordMap) {

                for ((index, str) in json["displayname"].asString.split(" ").withIndex()) {
                    val cleanedStr = str.cleanString()
                    val internalMap = titleWordMap.getOrPut(cleanedStr) { TreeMap() }
                    val indexList = internalMap.getOrPut(internalName) { mutableListOf() }
                    indexList.add(index)
                }
            }
        }
        return json
    }

    fun registerRecipe(recipe: PrimitiveRecipe) {
        for (internalName in recipe.outputs) {
            val recipeSet = recipesMap.getOrPut(internalName.internalName) { mutableSetOf() }
            recipeSet.add(recipe)
        }
    }

    fun getItemById(id: String): JsonObject? {
        return itemMap[id]
    }

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

    private val nbtListRegex = Regex("([\\[,])\\d+:")

    private fun convertNbtToJson(nbtString: String): NeuNbtInfoJson? {
        var convertedNbt = nbtString
        convertedNbt = convertedNbt.replace(nbtListRegex, "$1")
        try {
            val json = ConfigManager.gson.fromJson(convertedNbt, JsonObject::class.java)
            val fromJson = ConfigManager.gson.fromJson(json, NeuNbtInfoJson::class.java)
            return fromJson
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error converting nbt to json", "malformed nbt" to convertedNbt, "original nbt" to nbtString)
        }

        return null
    }

    fun jsonToStack(json: JsonObject?, useCache: Boolean = true, useReplacements: Boolean = false): ItemStack {
        //#if MC < 1.21
        json ?: return ItemStack(Items.painting)
        var usingCache = useCache && !useReplacements
        val internalName = json["internalname"].asString
        if (internalName == "_") usingCache = false

        if (usingCache) {
            val cachedStack = itemStackCache[internalName]
            if (cachedStack != null) return cachedStack.copy()
        }

        // todo modern doesnt have the "meta" number
        val stack = ItemStack(json["itemid"].asString.getVanillaItem() ?: return ItemStack(Item.getItemFromBlock(Blocks.stone), 0, 255))
        stack.item ?: return ItemStack(Item.getItemFromBlock(Blocks.stone), 0, 255)

        json["count"]?.asInt?.let { stack.stackSize = it }
        json["damage"]?.asInt?.let { stack.itemDamage = it }
        try {
            val nbtString = json["nbttag"]?.let { rawJsonNbt ->
                if (rawJsonNbt.isJsonObject) rawJsonNbt.toString()
                else rawJsonNbt.asString
            }
            val tag = JsonToNBT.getTagFromJson(nbtString)
            stack.tagCompound = tag
        } catch (_: Exception) {
            println("json was malformed: ${json["nbttag"]}")
            println("whole json: $json")
        }

        var replacements = mapOf<String, String>()
        if (useReplacements) {
            replacements = getPetLoreReplacements(stack, -1)
            json["displayname"]?.asString?.let {
                var name = it
                for ((key, value) in replacements) {
                    name = name.replace("{$key}", value)
                }
                stack.setCustomItemName(name)
            }
        }

        json["lore"]?.asJsonArray?.let { lore ->
            val displayTag = stack.tagCompound?.getCompoundTag("display") ?: NBTTagCompound()
            displayTag.setTag("Lore", processLore(lore, replacements))
            val tag = stack.tagCompound ?: NBTTagCompound()
            tag.setTag("display", displayTag)
            stack.tagCompound = tag
        }

        if (usingCache) itemStackCache[internalName] = stack
        return stack.copy()
        //#else
        //$$ json ?: return ItemStack(Items.PAINTING)
        //$$ var usingCache = useCache && !useReplacements
        //$$ val internalName = json["internalname"].asString
        //$$ if (internalName == "_") usingCache = false
        //$$
        //$$ if (usingCache) {
        //$$     val cachedStack = itemStackCache[internalName]
        //$$     if (cachedStack != null) return cachedStack.copy()
        //$$ }
        //$$
        //$$ val damage = json["damage"]?.asInt ?: 0
        //$$ val item: Item = ComponentUtils.convertMinecraftIdToModern(json["itemid"].asString, damage).getVanillaItem() ?: run {
        //$$     println(json["itemid"].asString + " " + damage + " is invalid item")
        //$$     return ItemStack(Blocks.STONE.asItem())
        //$$ }
        //$$ val stack = ItemStack(item)
        //$$ if (stack.item == Items.AIR) {
        //$$     return ItemStack(Blocks.STONE.asItem())
        //$$ }
        //$$
        //$$ json["count"]?.asInt?.let { stack.count = it }
        //$$
        //$$
        //$$ if (json["nbttag"]?.isJsonObject == false) {
        //$$     json["nbttag"]?.asString?.let { nbt ->
        //$$         ComponentUtils.convertToComponents(stack, convertNbtToJson(nbt))
        //$$     }
        //$$ } else {
        //$$     val neuNbtInfoJson = ConfigManager.gson.fromJson(json["nbttag"], NeuNbtInfoJson::class.java)
        //$$     ComponentUtils.convertToComponents(stack, neuNbtInfoJson)
        //$$ }
        //$$
        //$$ var replacements = mapOf<String, String>()
        //$$ if (useReplacements) {
        //$$     replacements = getPetLoreReplacements(stack, -1)
        //$$     json["displayname"]?.asString?.let {
        //$$         var name = it
        //$$         for ((key, value) in replacements) {
        //$$             name = name.replace("{$key}", value)
        //$$         }
        //$$         stack.setCustomItemName(name)
        //$$     }
        //$$ }
        //$$
        //$$ json["lore"]?.asJsonArray?.let { lore ->
        //$$     val loreList: MutableList<String> = mutableListOf()
        //$$     for (nbtElement in processLore(lore, replacements)) {
        //$$         loreList.add(nbtElement.asString().get())
        //$$     }
        //$$
        //$$     stack.setLore(loreList)
        //$$ }
        //$$
        //$$ if (usingCache) itemStackCache[internalName] = stack
        //$$ return stack.copy()
        //#endif
    }

    private fun getPetLoreReplacements(stack: ItemStack?, level: Int): Map<String, String> {
        stack?.tagCompound ?: return emptyMap()
        var petName: String? = null
        var tier: String? = null

        val extraAttributes = stack.extraAttributes
        if (extraAttributes.hasKey("petInfo")) {
            val petInfoStr = extraAttributes.getString("petInfo")
            val petInfo = ConfigManager.gson.fromJson(petInfoStr, JsonObject::class.java)
            petName = petInfo["name"]?.asString
            tier = petInfo["tier"]?.asString
            petInfo["heldItem"]?.asString?.let { it == "PET_ITEM_TIER_BOOST" }?.let {
                tier = when (tier) {
                    "COMMON" -> "UNCOMMON"
                    "UNCOMMON" -> "RARE"
                    "RARE" -> "EPIC"
                    "EPIC" -> "LEGENDARY"
                    else -> "MYTHIC"
                }
            }
        }

        return getPetLoreReplacements(petName, tier, level)
    }

    private fun getPetLoreReplacements(petName: String?, tier: String?, level: Int): Map<String, String> {

        val replacements = mutableMapOf<String, String>()

        if (level != -1) {
            replacements["LVL"] = level.toString()
        } else {
            neuPetsJson?.customPetLeveling?.get(petName)?.let { petLeveling ->
                val maxLevel = petLeveling.maxLevel ?: 100
                replacements["LVL"] = "1➡$maxLevel"
            } ?: run { replacements["LVL"] = "1➡100" }
        }

        if (petName == null || tier == null) return replacements

        val petNums = neuPetNums ?: return replacements
        petNums[petName]?.asJsonObject?.let { petInfo ->
            petInfo[tier]?.asJsonObject?.let { petInfoTier ->
                val min = petInfoTier["1"]?.asJsonObject ?: return replacements
                val max = petInfoTier["100"]?.asJsonObject ?: return replacements

                if (level < 1) {
                    val otherNumsMin = min["otherNums"]?.asJsonArray ?: return replacements
                    val otherNumsMax = max["otherNums"]?.asJsonArray ?: return replacements
                    var addZero = false
                    petInfoTier["stats_levelling_curve"]?.asString?.split(":")?.let { split ->
                        if (split.size == 3 && split[2].toInt() == 1) {
                            addZero = true
                        }
                    }
                    for (i in 0..otherNumsMax.size()) {
                        val start = if (addZero) "0➡" else ""
                        replacements[i.toString()] = "$start${otherNumsMin[i].asDouble}➡${otherNumsMax[i].asDouble}"
                    }
                    for (entry in max["statNums"].asJsonObject.entrySet()) {
                        val statMax = entry.value.asDouble
                        val statMin = min["statNums"].asJsonObject[entry.key].asDouble
                        val start = "${if (addZero) "0➡" else ""}${if (statMin > 0) "+" else ""}"
                        replacements[entry.key] = "$start$statMin➡$statMax"
                    }
                } else {
                    var minStatsLevel = 0
                    var maxStatsLevel = 100
                    var statLevelingType = -1

                    var statsLevel = level

                    petInfoTier["stats_levelling_curve"]?.asString?.split(":")?.let { split ->
                        if (split.size == 3) {
                            minStatsLevel = split[0].toInt()
                            maxStatsLevel = split[1].toInt()
                            statLevelingType = split[2].toInt()

                            if (statLevelingType in 0..1) {
                                statsLevel = if (level < minStatsLevel) {
                                    1
                                } else if (level < maxStatsLevel) {
                                    level - minStatsLevel + 1
                                } else {
                                    maxStatsLevel - minStatsLevel + 1
                                }
                            }
                        }
                    }

                    val minMix = (maxStatsLevel - (minStatsLevel - (if (statLevelingType == -1) 0 else 1)) - statsLevel) / 99f
                    val maxMix = (statsLevel - 1) / 99f

                    val otherNumsMin = min["otherNums"].asJsonArray
                    val otherNumsMax = max["otherNums"].asJsonArray
                    for (i in 0 until otherNumsMax.size()) {
                        val num = otherNumsMin[i].asFloat * minMix + otherNumsMax[i].asFloat * maxMix
                        if (statLevelingType == 1 && level < minStatsLevel) {
                            replacements[i.toString()] = "0"
                        } else {
                            replacements[i.toString()] = (floor((num * 10).toDouble()) / 10f).removeUnusedDecimal()
                        }
                    }

                    for ((key, value) in max["statNums"].asJsonObject.entrySet()) {
                        if (statLevelingType == 1 && level < minStatsLevel) {
                            replacements[key] = "0"
                        } else {
                            val statMax = value.asFloat
                            val statMin = min["statNums"].asJsonObject[key].asFloat
                            val num = statMin * minMix + statMax * maxMix
                            val statStr = (if (statMin > 0) "+" else "") + (floor((num * 10).toDouble()) / 10).removeUnusedDecimal()
                            replacements[key] = statStr
                        }
                    }
                }
            }
        }

        return replacements
    }

    private fun processLore(lore: JsonArray, replacements: Map<String, String>): NBTTagList {
        val loreList = NBTTagList()
        for (line in lore) {
            val loreLine = line.asString
            for ((key, value) in replacements) {
                loreLine.replace("{$key}", value)
            }
            //#if MC < 1.21
            loreList.appendTag(NBTTagString(loreLine))
            //#else
            //$$ loreList.add(NbtString.of(loreLine))
            //#endif
        }
        return loreList
    }

    fun getDisplayName(internalName: String): String {
        return displayNameCache.getOrPut(internalName) {
            val itemInfo = getItemById(internalName) ?: return@getOrPut internalName
            itemInfo["displayname"]?.asString ?: run {
                ErrorManager.skyHanniError("No displayname for $internalName")
            }
        }
    }

    private fun itemCountInRepoFolder(): Int {
        val itemsFolder = File(repoDirectory, "items")
        return itemsFolder.listFiles()?.size ?: 0
    }

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        neuPetsJson = event.getConstant<NeuPetsJson>("pets")
        neuPetNums = event.getConstant<JsonObject>("petnums")
        if (itemMap.isNotEmpty()) {
            ChatUtils.chat("Reloaded ${itemMap.size} items in the NEU repo")
        }
    }

    fun reportItemStatus() {
        val loadedItems = itemMap.size
        val directorySize = itemCountInRepoFolder()

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
