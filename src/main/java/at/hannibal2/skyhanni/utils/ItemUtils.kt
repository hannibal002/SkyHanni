package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.LorenzUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.init.Items
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.Constants
import java.util.*

object ItemUtils {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun ItemStack.cleanName() = this.displayName.removeColor()

    fun getItemsInOpenChest(): List<Slot> {
        val list = mutableListOf<Slot>()
        val guiChest = Minecraft.getMinecraft().currentScreen as GuiChest
        val inventorySlots = guiChest.inventorySlots.inventorySlots
        val skipAt = inventorySlots.size - 9 * 4
        var i = 0
        for (slot in inventorySlots) {
            val stack = slot.stack
            if (stack != null) {
                list.add(slot)
            }
            i++
            if (i == skipAt) break
        }
        return list
    }

    fun isSack(name: String): Boolean = name.endsWith(" Sack")//TODO change

    fun ItemStack.getLore() = getLoree(this)


    fun getLoree(`is`: ItemStack): List<String> {
        val tagCompound = `is`.tagCompound ?: return emptyList()
        val tagList = tagCompound.getCompoundTag("display").getTagList("Lore", 8)
        val list: MutableList<String> = ArrayList()
        for (i in 0 until tagList.tagCount()) {
            list.add(tagList.getStringTagAt(i))
        }
        return list
    }

    fun isCoopSoulBound(stack: ItemStack): Boolean =
        stack.getLore().any {
            it == "§8§l* §8Co-op Soulbound §8§l*" || it == "§8§l* §8Soulbound §8§l*"
        }

    fun isSoulBound(stack: ItemStack): Boolean =
        stack.getLore().any { it == "§8§l* §8Soulbound §8§l*" }

    fun isRecombobulated(stack: ItemStack): Boolean = stack.getLore().any { it.contains("§k") }//TODO use item api

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

    fun ItemStack.getInternalName(): String {
        val tag = tagCompound
        if (tag == null || !tag.hasKey("ExtraAttributes", 10)) return ""

        val extraAttributes = tag.getCompoundTag("ExtraAttributes")
        var internalName = if (extraAttributes.hasKey("id", 8)) {
            extraAttributes.getString("id").replace(":".toRegex(), "-")
        } else {
            return ""
        }

        if (internalName == "PET") {
            val petInfo = extraAttributes.getString("petInfo")
            if (petInfo.isNotEmpty()) {
                val petInfoObject: JsonObject = gson.fromJson(petInfo, JsonObject::class.java)
                internalName = petInfoObject["type"].asString
                return when (petInfoObject["tier"].asString) {
                    "COMMON" -> "$internalName;0"
                    "UNCOMMON" -> "$internalName;1"
                    "RARE" -> "$internalName;2"
                    "EPIC" -> "$internalName;3"
                    "LEGENDARY" -> "$internalName;4"
                    "MYTHIC" -> "$internalName;5"
                    else -> internalName
                }
            }
        }

        if (internalName == "ENCHANTED_BOOK" && extraAttributes.hasKey("enchantments", 10)) {
            val enchants = extraAttributes.getCompoundTag("enchantments")
            for (enchantment in enchants.keySet) {
                return enchantment.uppercase(Locale.getDefault()) + ";" + enchants.getInteger(enchantment)
            }
        }

        if (internalName == "RUNE" && extraAttributes.hasKey("runes", 10)) {
            val rune = extraAttributes.getCompoundTag("runes")
            for (enchantment in rune.keySet) {
                return enchantment.uppercase(Locale.getDefault()) + "_RUNE" + ";" + rune.getInteger(enchantment)
            }
        }

        if (internalName == "PARTY_HAT_CRAB" && extraAttributes.getString("party_hat_color") != null) {
            val crabHat = extraAttributes.getString("party_hat_color")
            return "PARTY_HAT_CRAB" + "_" + crabHat.uppercase(Locale.getDefault())
        }

        return internalName
    }

    fun ItemStack.getSkullTexture(): String? {
        if (item != Items.skull) return null
        if (tagCompound == null) return null
        val nbt = tagCompound
        if (!nbt.hasKey("SkullOwner")) return null
        return nbt.getCompoundTag("SkullOwner").getCompoundTag("Properties")
            .getTagList("textures", Constants.NBT.TAG_COMPOUND).getCompoundTagAt(0).getString("Value")
    }

    //extra method for shorter name and kotlin nullability logic
    var ItemStack.name: String?
        get() = this.displayName
        set(value) {
            setStackDisplayName(value)
        }
}