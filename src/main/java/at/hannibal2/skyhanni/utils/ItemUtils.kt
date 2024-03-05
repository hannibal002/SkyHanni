package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.PetAPI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.cachedData
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRecombobulated
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.common.util.Constants
import java.util.LinkedList
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.seconds

object ItemUtils {

    private val itemNameCache = mutableMapOf<NEUInternalName, String>() // internal name -> item name

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

    // TODO change else janni is sad
    fun isCoopSoulBound(stack: ItemStack): Boolean =
        stack.getLore().any {
            it == "§8§l* §8Co-op Soulbound §8§l*" || it == "§8§l* §8Soulbound §8§l*"
        }

    // TODO change else janni is sad
    fun isSoulBound(stack: ItemStack): Boolean =
        stack.getLore().any { it == "§8§l* §8Soulbound §8§l*" }

    fun isRecombobulated(stack: ItemStack) = stack.isRecombobulated()

    fun maxPetLevel(name: String) = if (name.contains("Golden Dragon")) 200 else 100

    fun getItemsInInventory(withCursorItem: Boolean = false): List<ItemStack> {
        val list: LinkedList<ItemStack> = LinkedList()
        val player = Minecraft.getMinecraft().thePlayer
        if (player == null) {
            ChatUtils.error("getItemsInInventoryWithSlots: player is null!")
            return list
        }
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

    fun getItemsInInventoryWithSlots(withCursorItem: Boolean = false): Map<ItemStack, Int> {
        val map: LinkedHashMap<ItemStack, Int> = LinkedHashMap()
        val player = Minecraft.getMinecraft().thePlayer
        if (player == null) {
            ChatUtils.error("getItemsInInventoryWithSlots: player is null!")
            return map
        }
        for (slot in player.openContainer.inventorySlots) {
            if (slot.hasStack) {
                map[slot.stack] = slot.slotNumber
            }
        }

        if (withCursorItem && player.inventory != null && player.inventory.itemStack != null) {
            map[player.inventory.itemStack] = -1
        }
        return map
    }

    fun hasAttributes(stack: ItemStack): Boolean {
        if (stack.hasTagCompound()) {
            val tagCompound = stack.tagCompound
            if (tagCompound.hasKey("ExtraAttributes")) {
                val extraAttributes = tagCompound.getCompoundTag("ExtraAttributes")
                try {
                    val json = GsonBuilder().create().fromJson(extraAttributes.toString(), JsonObject::class.java)
                    if (json.has("attributes")) {
                        return true
                    }
                } catch (_: Exception) {
                }
            }
        }
        return false
    }

    fun ItemStack.getInternalName() = getInternalNameOrNull() ?: NEUInternalName.NONE

    fun ItemStack.getInternalNameOrNull(): NEUInternalName? {
        val data = cachedData
        if (data.lastInternalNameFetchTime.asTimeMark().passedSince() < 1.seconds) {
            return data.lastInternalName
        }
        val internalName = grabInternalNameOrNull()
        data.lastInternalName = internalName
        data.lastInternalNameFetchTime = SimpleTimeMark.now().toMillis()
        return internalName
    }

    private fun ItemStack.grabInternalNameOrNull(): NEUInternalName? {
        if (name == "§fWisp's Ice-Flavored Water I Splash Potion") {
            return NEUInternalName.WISP_POTION
        }
        return NEUItems.getInternalName(this)?.asInternalName()
    }

    fun ItemStack.isVanilla() = NEUItems.isVanillaItem(this)

    // Checks for the enchantment glint as part of the minecraft enchantments
    fun ItemStack.isEnchanted() = isItemEnchanted

    // Checks for hypixel enchantments in the attributes
    fun ItemStack.hasEnchantments() = getEnchantments()?.isNotEmpty() ?: false

    fun ItemStack.getSkullTexture(): String? {
        if (item != Items.skull) return null
        if (tagCompound == null) return null
        val nbt = tagCompound
        if (!nbt.hasKey("SkullOwner")) return null
        return nbt.getCompoundTag("SkullOwner").getCompoundTag("Properties")
            .getTagList("textures", Constants.NBT.TAG_COMPOUND).getCompoundTagAt(0).getString("Value")
    }

    fun ItemStack.getSkullOwner(): String? {
        if (item != Items.skull) return null
        if (tagCompound == null) return null
        val nbt = tagCompound
        if (!nbt.hasKey("SkullOwner")) return null
        return nbt.getCompoundTag("SkullOwner").getString("Id")
    }

    // Taken from NEU
    fun createSkull(displayName: String, uuid: String, value: String): ItemStack {
        return createSkull(displayName, uuid, value, null)
    }

    // Taken from NEU
    fun createSkull(displayName: String, uuid: String, value: String, lore: Array<String>?): ItemStack {
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

        addNameAndLore(tag, displayName, lore)

        properties.setTag("textures", textures)
        skullOwner.setTag("Properties", properties)
        tag.setTag("SkullOwner", skullOwner)
        render.tagCompound = tag
        return render
    }

    // Taken from NEU
    private fun addNameAndLore(tag: NBTTagCompound, displayName: String, lore: Array<String>?) {
        val display = NBTTagCompound()
        display.setString("Name", displayName)
        if (lore != null) {
            val tagLore = NBTTagList()
            for (line in lore) {
                tagLore.appendTag(NBTTagString(line))
            }
            display.setTag("Lore", tagLore)
        }
        tag.setTag("display", display)
    }

    fun ItemStack.getItemRarityOrCommon() = getItemRarityOrNull() ?: LorenzRarity.COMMON

    private fun ItemStack.readItemCategoryAndRarity(): Pair<LorenzRarity?, ItemCategory?> {
        val name = this.name ?: ""
        val cleanName = this.cleanName()

        if (PetAPI.hasPetName(cleanName)) {
            return getPetRarity(this) to ItemCategory.PET
        }

        for (line in this.getLore().reversed()) {
            val (category, rarity) = UtilsPatterns.rarityLoreLinePattern.matchMatcher(line) {
                group("itemCategory").replace(" ", "_") to
                    group("rarity").replace(" ", "_")
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
                )
            }
            if (itemRarity == null) {
                ErrorManager.logErrorStateWithData(
                    "Could not read rarity for item $name",
                    "Failed to read rarity from item rarity via item lore",
                    "internal name" to getInternalName(),
                    "item name" to name,
                    "inventory name" to InventoryUtils.openInventoryName(),
                    "lore" to getLore(),
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
            UtilsPatterns.enchantedBookPattern.matches(name) -> ItemCategory.ENCHANTED_BOOK
            UtilsPatterns.potionPattern.matches(name) -> ItemCategory.POTION
            UtilsPatterns.sackPattern.matches(name) -> ItemCategory.SACK
            else -> ItemCategory.NONE
        } else {
            LorenzUtils.enumValueOfOrNull<ItemCategory>(itemCategory)
        }

    private fun ItemStack.updateCategoryAndRarity() {
        val data = cachedData
        data.itemRarityLastCheck = SimpleTimeMark.now().toMillis()
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

    private fun itemRarityLastCheck(data: CachedItemData) =
        data.itemRarityLastCheck.asTimeMark().passedSince() > 10.seconds

    // extra method for shorter name and kotlin nullability logic
    var ItemStack.name: String?
        get() = this.displayName
        set(value) {
            setStackDisplayName(value)
        }

    @Deprecated("outdated", ReplaceWith("itemName"))
    val ItemStack.nameWithEnchantment: String?
        get() = getInternalNameOrNull()?.itemName

    fun isSkyBlockMenuItem(stack: ItemStack?): Boolean = stack?.getInternalName()?.equals("SKYBLOCK_MENU") ?: false

    private val itemAmountCache = mutableMapOf<String, Pair<String, Int>>()

    fun readItemAmount(originalInput: String): Pair<String, Int>? {
        // This workaround fixes 'Tubto Cacti I Book'
        val input = if (originalInput.endsWith(" Book")) {
            originalInput.replace(" Book", "")
        } else originalInput

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
            println("")
            println("input: '$input'")
            println("string: '$string'")
            return null
        }

        val itemName = color + matcher.group("name").trim()
        return makePair(input, itemName, matcher)
    }

    private fun makePair(input: String, itemName: String, matcher: Matcher): Pair<String, Int> {
        val matcherAmount = matcher.group("amount")
        val amount = matcherAmount?.formatNumber()?.toInt() ?: 1
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
                "inventory name" to InventoryUtils.openInventoryName()
            )
        }
        return rarity
    }

    fun NEUInternalName.isRune(): Boolean = contains("_RUNE;")

    val ItemStack.itemName: String
        get() = getInternalName().itemName

    val ItemStack.itemNameWithoutColor: String get() = itemName.removeColor()

    val NEUInternalName.itemName: String
        get() = itemNameCache.getOrPut(this) { grabItemName() }

    val NEUInternalName.itemNameWithoutColor: String get() = itemName.removeColor()

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

        val itemStack = getItemStackOrNull()
        val name = itemStack?.name ?: error("Could not find item name for $this")

        // show enchanted book name
        if (name.endsWith("Enchanted Book")) {
            return itemStack.getLore()[0]
        }

        // hide pet level
        PetAPI.getCleanName(name)?.let {
            return "$it Pet"
        }
        return name
    }
}
