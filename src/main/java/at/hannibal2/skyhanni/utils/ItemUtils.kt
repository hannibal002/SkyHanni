package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.cachedData
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRecombobulated
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
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

    fun isPet(name: String): Boolean = name.matchRegex("\\[Lvl (.*)] (.*)") && !listOf(
        "Archer",
        "Berserk",
        "Mage",
        "Tank",
        "Healer",
        "➡",
    ).any { name.contains(it) }

    fun maxPetLevel(name: String) = if (name.contains("Golden Dragon")) 200 else 100

    fun getItemsInInventory(withCursorItem: Boolean = false): List<ItemStack> {
        val list: LinkedList<ItemStack> = LinkedList()
        val player = Minecraft.getMinecraft().thePlayer
        if (player == null) {
            LorenzUtils.error("getItemsInInventoryWithSlots: player is null!")
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
            LorenzUtils.error("getItemsInInventoryWithSlots: player is null!")
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

    // TODO remove
    @Deprecated("Use NEUInternalName rather than String", ReplaceWith("getInternalName()"))
    fun ItemStack.getInternalName_old() = getInternalName().asString()

    fun ItemStack.getInternalName() = getInternalNameOrNull() ?: NEUInternalName.NONE

    fun ItemStack.getInternalNameOrNull() = getRawInternalName()?.asInternalName()

    private fun ItemStack.getRawInternalName(): String? {
        if (name == "§fWisp's Ice-Flavored Water I Splash Potion") {
            return "WISP_POTION"
        }
        return NEUItems.getInternalName(this)
    }

    fun ItemStack.isVanilla() = NEUItems.isVanillaItem(this)

    fun ItemStack.isEnchanted() = isItemEnchanted

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
    private fun createSkull(displayName: String, uuid: String, value: String, lore: Array<String>?): ItemStack {
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

    fun ItemStack.getItemRarityOrNull(logError: Boolean = true): LorenzRarity? {
        val data = cachedData
        if (data.itemRarityLastCheck.asTimeMark().passedSince() < 1.seconds) {
            return data.itemRarity
        }
        data.itemRarityLastCheck = SimpleTimeMark.now().toMillis()

        val internalName = getInternalName()
        if (internalName == NEUInternalName.NONE) {
            data.itemRarity = null
            return null
        }


        if (isPet(cleanName())) {
            val petRarity = getPetRarity(this)
            data.itemRarity = petRarity
            return petRarity
        }

        val rarity = LorenzRarity.readItemRarity(this)
        data.itemRarity = rarity
        if (rarity == null && logError) {
            ErrorManager.logErrorStateWithData(
                "Could not read rarity for item $name",
                "Failed to read rarity from item rarity via item lore",
                "internal name" to internalName,
                "item name" to name,
                "inventory name" to InventoryUtils.openInventoryName(),
                "lore" to getLore(),
            )
        }
        return rarity
    }

    //extra method for shorter name and kotlin nullability logic
    var ItemStack.name: String?
        get() = this.displayName
        set(value) {
            setStackDisplayName(value)
        }

    val ItemStack.nameWithEnchantment: String?
        get() = name?.let {
            if (it.endsWith("Enchanted Book")) {
                getLore()[0]
            } else it
        }

    fun isSkyBlockMenuItem(stack: ItemStack?): Boolean = stack?.getInternalName()?.equals("SKYBLOCK_MENU") ?: false

    // TODO USE SH-REPO
    private val patternInFront = "(?: *§8(\\+§[\\d\\w])?(?<amount>[\\d\\.km,]+)(x )?)?(?<name>.*)".toPattern()
    private val patternBehind = "(?<name>(?:['\\w-]+ ?)+)(?:§8x(?<amount>[\\d,]+))?".toPattern()

    private val itemAmountCache = mutableMapOf<String, Pair<String, Int>>()

    fun readItemAmount(originalInput: String): Pair<String, Int>? {
        // This workaround fixes 'Tubto Cacti I Book'
        val input = if (originalInput.endsWith(" Book")) {
            originalInput.replace(" Book", "")
        } else originalInput

        if (itemAmountCache.containsKey(input)) {
            return itemAmountCache[input]!!
        }

        var matcher = patternInFront.matcher(input)
        if (matcher.matches()) {
            val itemName = matcher.group("name")
            if (!itemName.contains("§8x")) {
                return makePair(input, itemName.trim(), matcher)
            }
        }

        var string = input.trim()
        val color = string.substring(0, 2)
        string = string.substring(2)
        matcher = patternBehind.matcher(string)
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

    fun NEUInternalName.getItemNameOrNull() = getItemStack().name

    fun NEUInternalName.getItemName() = getItemNameOrNull() ?: error("No item name found for $this")

    fun NEUInternalName.getNameWithEnchantment(): String {
        if (equals("WISP_POTION")) {
            return "§fWisp's Ice-Flavored Water"
        }
        return getItemStack().nameWithEnchantment ?: error("Could not find item name for $this")
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
}
