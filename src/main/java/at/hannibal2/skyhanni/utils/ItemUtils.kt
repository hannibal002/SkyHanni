package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.PetAPI
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator.getAttributeName
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.cachedData
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getAttributes
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRecombobulated
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.common.collect.Lists
import io.github.moulberry.notenoughupdates.recipes.NeuRecipe
import io.github.moulberry.notenoughupdates.util.NotificationHandler
import net.minecraft.client.Minecraft
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.LinkedList
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ItemUtils {

    private val itemNameCache = mutableMapOf<NEUInternalName, String>() // internal name -> item name

    private val missingRepoItems = mutableSetOf<String>()

    fun ItemStack.cleanName() = this.displayName.removeColor()

    fun isSack(stack: ItemStack) = stack.getInternalName().endsWith("_SACK") && stack.cleanName().endsWith(" Sack")

    fun ItemStack.getLore(): List<String> {
        val tagCompound = this.tagCompound ?: return emptyList()
        val tagList = tagCompound.getCompoundTag("display").getTagList("Lore", 8)
        val list: MutableList<String> = ArrayList()
        for (i in 0 until tagList.tagCount()) {
            list.add(tagList.getStringTagAt(i))
        }
        return list
    }

    var ItemStack.extraAttributes: NBTTagCompound
        get() = this.tagCompound?.getCompoundTag("ExtraAttributes") ?: NBTTagCompound()
        set(value) {
            val tag = this.tagCompound ?: NBTTagCompound().also { tagCompound = it }
            tag.setTag("ExtraAttributes", value)
        }

    fun ItemStack.overrideId(id: String): ItemStack {
        extraAttributes = extraAttributes.apply { setString("id", id) }
        return this
    }

    // TODO change else janni is sad
    fun ItemStack.isCoopSoulBound(): Boolean = getLore().any {
        it == "§8§l* §8Co-op Soulbound §8§l*" || it == "§8§l* §8Soulbound §8§l*"
    }

    // TODO change else janni is sad
    fun ItemStack.isSoulBound(): Boolean = getLore().any { it == "§8§l* §8Soulbound §8§l*" }

    fun isRecombobulated(stack: ItemStack) = stack.isRecombobulated()

    fun maxPetLevel(name: String) = if (name.contains("Golden Dragon")) 200 else 100

    fun getItemsInInventory(withCursorItem: Boolean = false): List<ItemStack> {
        val list: LinkedList<ItemStack> = LinkedList()
        val player = Minecraft.getMinecraft().thePlayer ?: ErrorManager.skyHanniError("getItemsInInventoryWithSlots: player is null!")

        for (slot in player.openContainer.inventorySlots) {
            if (slot.hasStack) {
                list.add(slot.stack)
            }
        }

        if (withCursorItem && player.inventory != null && player.inventory.itemStack != null) {
            list.add(player.inventory.itemStack)
        }
        return list
    }

    fun ItemStack.getInternalName() = getInternalNameOrNull() ?: NEUInternalName.NONE

    fun ItemStack.getInternalNameOrNull(): NEUInternalName? {
        val data = cachedData
        if (data.lastInternalNameFetchTime.passedSince() < 1.seconds) {
            return data.lastInternalName
        }
        val internalName = grabInternalNameOrNull()
        data.lastInternalName = internalName
        data.lastInternalNameFetchTime = SimpleTimeMark.now()
        return internalName
    }

    private fun ItemStack.grabInternalNameOrNull(): NEUInternalName? {
        if (name == "§fWisp's Ice-Flavored Water I Splash Potion") {
            return NEUInternalName.WISP_POTION
        }
        val internalName = NEUItems.getInternalName(this)?.replace("ULTIMATE_ULTIMATE_", "ULTIMATE_")
        return internalName?.asInternalName()
    }

    fun ItemStack.isVanilla() = NEUItems.isVanillaItem(this)

    // Checks for the enchantment glint as part of the minecraft enchantments
    fun ItemStack.isEnchanted() = isItemEnchanted

    // Checks for hypixel enchantments in the attributes
    fun ItemStack.hasEnchantments() = getEnchantments()?.isNotEmpty() ?: false

    fun ItemStack.removeEnchants(): ItemStack = apply {
        val tag = tagCompound ?: NBTTagCompound()
        tag.removeTag("ench")
        tag.removeTag("StoredEnchantments")
        tagCompound = tag
    }

    fun ItemStack.getSkullTexture(): String? {
        if (item != Items.skull) return null
        if (tagCompound == null) return null
        val nbt = tagCompound
        if (!nbt.hasKey("SkullOwner")) return null
        return nbt.getCompoundTag("SkullOwner").getCompoundTag("Properties").getTagList("textures", Constants.NBT.TAG_COMPOUND)
            .getCompoundTagAt(0).getString("Value")
    }

    fun ItemStack.getSkullOwner(): String? {
        if (item != Items.skull) return null
        if (tagCompound == null) return null
        val nbt = tagCompound
        if (!nbt.hasKey("SkullOwner")) return null
        return nbt.getCompoundTag("SkullOwner").getString("Id")
    }

    // Taken from NEU
    fun createSkull(displayName: String, uuid: String, value: String, vararg lore: String): ItemStack {
        val render = ItemStack(Items.skull, 1, 3)
        val tag = NBTTagCompound()
        val skullOwner = NBTTagCompound()
        val properties = NBTTagCompound()
        val textures = NBTTagList()
        val textures0 = NBTTagCompound()

        skullOwner.setString("Id", uuid)
        skullOwner.setString("Name", uuid)
        textures0.setString("Value", value)

        textures.appendTag(textures0)

        addNameAndLore(tag, displayName, *lore)

        properties.setTag("textures", textures)
        skullOwner.setTag("Properties", properties)
        tag.setTag("SkullOwner", skullOwner)
        render.tagCompound = tag
        return render
    }

    // Taken from NEU
    private fun addNameAndLore(tag: NBTTagCompound, displayName: String, vararg lore: String) {
        val display = NBTTagCompound()
        display.setString("Name", displayName)
        if (lore.isNotEmpty()) {
            val tagLore = NBTTagList()
            for (line in lore) {
                tagLore.appendTag(NBTTagString(line))
            }
            display.setTag("Lore", tagLore)
        }
        tag.setTag("display", display)
    }

    fun ItemStack.getItemRarityOrCommon() = getItemRarityOrNull() ?: LorenzRarity.COMMON

    private val itemCategoryRepoCheckPattern by RepoPattern.pattern(
        "itemcategory.repocheck",
        ItemCategory.entries.joinToString(separator = "|") { it.name },
    )
    private val rarityCategoryRepoCheckPattern by RepoPattern.pattern(
        "rarity.repocheck",
        LorenzRarity.entries.joinToString(separator = "|") { it.name },
    )

    private fun ItemStack.readItemCategoryAndRarity(): Pair<LorenzRarity?, ItemCategory?> {
        val cleanName = this.cleanName()

        if (PetAPI.hasPetName(cleanName)) {
            return getPetRarity(this) to ItemCategory.PET
        }

        for (line in this.getLore().reversed()) {
            val (category, rarity) = UtilsPatterns.rarityLoreLinePattern.matchMatcher(line) {
                group("itemCategory").replace(" ", "_") to group("rarity").replace(" ", "_")
            } ?: continue

            val itemCategory = getItemCategory(category, name, cleanName)
            val itemRarity = LorenzRarity.getByName(rarity)

            if (itemCategory == null) {
                ErrorManager.logErrorStateWithData(
                    "Could not read category for item $name",
                    "Failed to read category from item rarity via item lore",
                    "internal name" to getInternalName(),
                    "item name" to name,
                    "inventory name" to InventoryUtils.openInventoryName(),
                    "pattern result" to category,
                    "lore" to getLore(),
                    betaOnly = true,
                    condition = { !itemCategoryRepoCheckPattern.matches(category) },
                )
            }
            if (itemRarity == null) {
                ErrorManager.logErrorStateWithData(
                    "Could not read rarity for item $name",
                    "Failed to read rarity from item rarity via item lore",
                    "internal name" to getInternalName(),
                    "item name" to name,
                    "inventory name" to InventoryUtils.openInventoryName(),
                    "pattern result" to rarity,
                    "lore" to getLore(),
                    betaOnly = true,
                    condition = { !rarityCategoryRepoCheckPattern.matches(rarity) },
                )
            }

            return itemRarity to itemCategory
        }
        return null to null
    }

    private fun getItemCategory(itemCategory: String, name: String, cleanName: String = name.removeColor()) =
        if (itemCategory.isEmpty()) when {
            UtilsPatterns.abiPhonePattern.matches(name) -> ItemCategory.ABIPHONE
            PetAPI.hasPetName(cleanName) -> ItemCategory.PET
            UtilsPatterns.baitPattern.matches(cleanName) -> ItemCategory.FISHING_BAIT
            UtilsPatterns.enchantedBookPattern.matches(name) -> ItemCategory.ENCHANTED_BOOK
            UtilsPatterns.potionPattern.matches(name) -> ItemCategory.POTION
            UtilsPatterns.sackPattern.matches(name) -> ItemCategory.SACK
            else -> ItemCategory.NONE
        } else {
            LorenzUtils.enumValueOfOrNull<ItemCategory>(itemCategory)
        }

    private fun ItemStack.updateCategoryAndRarity() {
        val data = cachedData
        data.itemRarityLastCheck = SimpleTimeMark.now()
        val internalName = getInternalName()
        if (internalName == NEUInternalName.NONE) {
            data.itemRarity = null
            data.itemCategory = null
            return
        }
        val pair = this.readItemCategoryAndRarity()
        data.itemRarity = pair.first
        data.itemCategory = pair.second
    }

    fun ItemStack.getItemCategoryOrNull(): ItemCategory? {
        val data = cachedData
        if (itemRarityLastCheck(data)) {
            this.updateCategoryAndRarity()
        }
        return data.itemCategory
    }

    fun ItemStack.getItemRarityOrNull(): LorenzRarity? {
        val data = cachedData
        if (itemRarityLastCheck(data)) {
            this.updateCategoryAndRarity()
        }
        return data.itemRarity
    }

    private fun itemRarityLastCheck(data: CachedItemData) = data.itemRarityLastCheck.passedSince() > 10.seconds

    /**
     * Use when comparing the name (e.g. regex), not for showing to the user
     * Member that provides the item name, is null save or throws visual error
     */
    var ItemStack.name: String
        get() = this.displayName ?: ErrorManager.skyHanniError(
            "Could not get name of ItemStack",
            "itemStack" to this,
            "displayName" to displayName,
            "internal name" to getInternalNameOrNull(),
        )
        set(value) {
            setStackDisplayName(value)
        }

    fun isSkyBlockMenuItem(stack: ItemStack?): Boolean = stack?.getInternalName()?.equals("SKYBLOCK_MENU") ?: false

    private val itemAmountCache = mutableMapOf<String, Pair<String, Int>>()

    private val bookPattern = "(?<name>.* [IVX]+) Book".toPattern()

    fun readItemAmount(originalInput: String): Pair<String, Int>? {
        // This workaround fixes 'Turbo Cacti I Book'
        val input = (bookPattern.matchMatcher(originalInput) { group("name") } ?: originalInput).removeResets()

        if (itemAmountCache.containsKey(input)) {
            return itemAmountCache[input]!!
        }

        UtilsPatterns.readAmountBeforePattern.matchMatcher(input) {
            val itemName = group("name")
            if (!itemName.contains("§8x")) {
                return makePair(input, itemName.trim(), this)
            }
        }

        var string = input.trim()
        val color = string.substring(0, 2)
        string = string.substring(2)
        val matcher = UtilsPatterns.readAmountAfterPattern.matcher(string)
        if (!matcher.matches()) {
            return null
        }

        val itemName = color + matcher.group("name").trim()
        return makePair(input, itemName, matcher)
    }

    private fun makePair(input: String, itemName: String, matcher: Matcher): Pair<String, Int> {
        val matcherAmount = matcher.group("amount")
        val amount = matcherAmount?.formatInt() ?: 1
        val pair = Pair(itemName, amount)
        itemAmountCache[input] = pair
        return pair
    }

    private fun getPetRarity(pet: ItemStack): LorenzRarity? {
        val rarityId = pet.getInternalName().asString().split(";").last().toInt()
        val rarity = LorenzRarity.getById(rarityId)
        val name = pet.name
        if (rarity == null) {
            ErrorManager.logErrorStateWithData(
                "Could not read rarity for pet $name",
                "Failed to read rarity from pet item via internal name",
                "internal name" to pet.getInternalName(),
                "item name" to name,
                "rarity id" to rarityId,
                "inventory name" to InventoryUtils.openInventoryName(),
            )
        }
        return rarity
    }

    fun NEUInternalName.isRune(): Boolean = contains("_RUNE;")

    // use when showing the item name to the user (in guis, chat message, etc.), not for comparing
    val ItemStack.itemName: String
        get() {
            getAttributeFromShard()?.let {
                return it.getAttributeName()
            }
            return getInternalNameOrNull()?.itemName ?: "<null>"
        }

    fun ItemStack.getAttributeFromShard(): Pair<String, Int>? {
        if (getInternalName().asString() != "ATTRIBUTE_SHARD") return null
        val attributes = getAttributes() ?: return null
        return attributes.firstOrNull()
    }

    val ItemStack.itemNameWithoutColor: String get() = itemName.removeColor()

    // use when showing the item name to the user (in guis, chat message, etc.), not for comparing
    val NEUInternalName.itemName: String
        get() = itemNameCache.getOrPut(this) { grabItemName() }

    val NEUInternalName.itemNameWithoutColor: String get() = itemName.removeColor()

    val NEUInternalName.readableInternalName: String
        get() = asString().replace("_", " ").lowercase()

    private fun NEUInternalName.grabItemName(): String {
        if (this == NEUInternalName.WISP_POTION) {
            return "§fWisp's Ice-Flavored Water"
        }
        if (this == NEUInternalName.SKYBLOCK_COIN) {
            return "§6Coins"
        }
        if (this == NEUInternalName.NONE) {
            error("NEUInternalName.NONE has no name!")
        }
        if (NEUItems.ignoreItemsFilter.match(this.asString())) {
            return "§cBugged Item"
        }

        val itemStack = getItemStackOrNull()
        val name = itemStack?.name ?: run {
            val name = toString()
            addMissingRepoItem(name, "Could not find item name for $name")
            return "§c$name"
        }

        // show enchanted book name
        if (itemStack.getItemCategoryOrNull() == ItemCategory.ENCHANTED_BOOK) {
            return itemStack.getLore()[0]
        }
        if (name.endsWith("Enchanted Book Bundle")) {
            return name.replace("Enchanted Book", itemStack.getLore()[0].removeColor())
        }

        // obfuscated trophy fish
        if (name.contains("§kObfuscated")) {
            return name.replace("§kObfuscated", "Obfuscated")
        }

        // hide pet level
        PetAPI.getCleanName(name)?.let {
            return "$it Pet"
        }
        return name
    }

    fun ItemStack.loreCosts(): MutableList<NEUInternalName> {
        var found = false
        val list = mutableListOf<NEUInternalName>()
        for (lines in getLore()) {
            if (lines == "§7Cost") {
                found = true
                continue
            }

            if (!found) continue
            if (lines.isEmpty()) return list

            NEUInternalName.fromItemNameOrNull(lines)?.let {
                list.add(it)
            }
        }
        return list
    }

    fun neededItems(recipe: NeuRecipe): Map<NEUInternalName, Int> {
        val neededItems = mutableMapOf<NEUInternalName, Int>()
        for (ingredient in recipe.ingredients) {
            val material = ingredient.internalItemId.asInternalName()
            val amount = ingredient.count.toInt()
            neededItems.addOrPut(material, amount)
        }
        return neededItems
    }

    fun NeuRecipe.getRecipePrice(
        priceSource: ItemPriceSource = ItemPriceSource.BAZAAR_INSTANT_BUY,
        pastRecipes: List<NeuRecipe> = emptyList(),
    ): Double = neededItems(this).map {
        it.key.getPrice(priceSource, pastRecipes) * it.value
    }.sum()

    @SubscribeEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Missing Repo Items")

        if (missingRepoItems.isNotEmpty()) {
            event.addData {
                add("Detected ${missingRepoItems.size} missing items:")
                for (itemName in missingRepoItems) {
                    add(" - $itemName")
                }
            }
        } else {
            event.addIrrelevant("No Repo Item fails detected.")
        }
    }

    fun addMissingRepoItem(name: String, message: String) {
        if (!missingRepoItems.add(name)) return
        ChatUtils.debug(message)
//         showRepoWarning()
    }

    // Running NEU's function `Utils.showOutdatedRepoNotification()` caused a NoSuchMethodError in dev env.
    // Therefore we run NotificationHandler.displayNotification directly
    private fun showRepoWarning() {
        NotificationHandler.displayNotification(
            Lists.newArrayList(
                "§c§lMissing repo data",
                "§cData used for some SkyHanni features is not up to date, this should normally not be the case.",
                "§cYou can try §l/neuresetrepo§r§c and restart your game to see if that fixes the issue.",
                "§cIf the problem persists please join the SkyHanni Discord and message in §l#support§r§c to get support.",
            ),
            true, true,
        )
    }
}
