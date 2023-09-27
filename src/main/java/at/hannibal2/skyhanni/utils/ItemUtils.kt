package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
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
import net.minecraftforge.common.util.Constants
import java.util.LinkedList
import kotlin.time.Duration.Companion.seconds

object ItemUtils {

    fun ItemStack.cleanName() = this.displayName.removeColor()

    fun isSack(stack: itemStack): Boolean =
        return stack.getInternalName().endsWith("_SACK") ?: false //TODO use item id or api or something? or dont, its working fine now

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
            return getPetRarity(this)
        }

        val rarity = LorenzRarity.readItemRarity(this)
        data.itemRarity = rarity
        if (rarity == null && logError) {
            CopyErrorCommand.logErrorState(
                "Could not read rarity for item $name",
                "getItemRarityOrNull not found for: $internalName, name:'$name''"
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

    fun isSkyBlockMenuItem(stack: ItemStack?): Boolean = stack?.getInternalName_old() == "SKYBLOCK_MENU"

    private val patternInFront = "(?: *§8(?<amount>[\\d,]+)x )?(?<name>.*)".toPattern()
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
            return null
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

    // TODO: Replace entirely some day
    fun getPetRarityOld(petStack: ItemStack?): Int {
        val rarity = petStack?.getItemRarityOrNull() ?: return -1

        return rarity.id
    }

    private fun getPetRarity(pet: ItemStack): LorenzRarity? {
        val rarityId = pet.getInternalName().asString().split(";").last().toInt()
        val rarity = LorenzRarity.getById(rarityId)
        val name = pet.name
        if (rarity == null) {
            CopyErrorCommand.logErrorState(
                "Could not read rarity for pet $name",
                "getPetRarity not found for: ${pet.getInternalName()}, name:'$name'"
            )
        }
        return rarity
    }
}