package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRecombobulated
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.Constants
import java.util.*

object ItemUtils {

    fun ItemStack.cleanName() = this.displayName.removeColor()

    fun isSack(name: String): Boolean =
        name.endsWith(" Sack")//TODO use item id or api or something? or dont, its working fine now

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
            LorenzUtils.warning("getItemsInInventoryWithSlots: player is null!")
            return list
        }
        for (slot in player.openContainer.inventorySlots) {
            if (slot.hasStack) {
                list.add(slot.stack)
            }
        }

        if (withCursorItem) {
            if (player.inventory != null) {
                if (player.inventory.itemStack != null) {
                    list.add(player.inventory.itemStack)
                }
            }
        }
        return list
    }

    fun getItemsInInventoryWithSlots(withCursorItem: Boolean = false): Map<ItemStack, Int> {
        val map: LinkedHashMap<ItemStack, Int> = LinkedHashMap()
        val player = Minecraft.getMinecraft().thePlayer
        if (player == null) {
            LorenzUtils.warning("getItemsInInventoryWithSlots: player is null!")
            return map
        }
        for (slot in player.openContainer.inventorySlots) {
            if (slot.hasStack) {
                map[slot.stack] = slot.slotNumber
            }
        }

        if (withCursorItem) {
            if (player.inventory != null) {
                if (player.inventory.itemStack != null) {
                    map[player.inventory.itemStack] = -1
                }
            }
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

    fun ItemStack.getItemRarity(): Int {
        //todo make into an enum in future
        return when (this.getLore().lastOrNull()?.take(4)) {
            "§f§l" -> 0     // common
            "§a§l" -> 1     // uncommon
            "§9§l" -> 2     // rare
            "§5§l" -> 3     // epic
            "§6§l" -> 4     // legendary
            "§d§l" -> 5     // mythic
            "§b§l" -> 6     // divine
            "§4§l" -> 7     // supreme
            "§c§l" -> 8     // special/very special
            else -> -1      // unknown
        }
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

    fun isSkyBlockMenuItem(stack: ItemStack?): Boolean = stack?.getInternalName_old() == "SKYBLOCK_MENU"

    private val patternInFront = "(?: *§8(?<amount>[\\d,]+)x )?(?<name>.*)".toPattern()
    private val patternBehind = "(?<name>(?:['\\w-]+ ?)+)(?:§8x(?<amount>[\\d,]+))?".toPattern()

    private val itemAmountCache = mutableMapOf<String, Pair<String, Int>>()

    fun readItemAmount(input: String): Pair<String?, Int> {
        if (itemAmountCache.containsKey(input)) {
            return itemAmountCache[input]!!
        }

        var matcher = patternInFront.matcher(input)
        if (matcher.matches()) {
            val itemName = matcher.group("name")
            if (!itemName.contains("§8x")) {
                val amount = matcher.group("amount")?.replace(",", "")?.toInt() ?: 1
                val pair = Pair(itemName.trim(), amount)
                itemAmountCache[input] = pair
                return pair
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
            return Pair(null, 0)
        }

        val itemName = color + matcher.group("name").trim()
        val amount = matcher.group("amount")?.replace(",", "")?.toInt() ?: 1
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
}