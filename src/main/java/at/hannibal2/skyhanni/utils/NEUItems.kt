package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.features.bazaar.BazaarDataHolder
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemBlink.checkBlinkItem
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import io.github.moulberry.notenoughupdates.NEUManager
import io.github.moulberry.notenoughupdates.NEUOverlay
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import io.github.moulberry.notenoughupdates.overlays.AuctionSearchOverlay
import io.github.moulberry.notenoughupdates.overlays.BazaarSearchOverlay
import io.github.moulberry.notenoughupdates.recipes.CraftingRecipe
import io.github.moulberry.notenoughupdates.recipes.NeuRecipe
import io.github.moulberry.notenoughupdates.util.ItemResolutionQuery
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import java.util.regex.Pattern

object NEUItems {
    val manager: NEUManager get() = NotEnoughUpdates.INSTANCE.manager
    private val itemNameCache = mutableMapOf<String, NEUInternalName>() // item name -> internal name
    private val multiplierCache = mutableMapOf<String, Pair<String, Int>>()
    private val recipesCache = mutableMapOf<String, Set<NeuRecipe>>()
    private val enchantmentNamePattern = Pattern.compile("^(?<format>(?:§.)+)(?<name>[^§]+) (?<level>[IVXL]+)$")
    var allItemsCache = mapOf<String, NEUInternalName>() // item name -> internal name
    var allInternalNames = mutableListOf<NEUInternalName>()

    private val fallbackItem by lazy {
        Utils.createItemStack(
            ItemStack(Blocks.barrier).item,
            "§cMissing Repo Item",
            "§cYour NEU repo seems to be out of date"
        )
    }

    // TODO remove
    @Deprecated("Use NEUInternalName rather than String", ReplaceWith("getInternalNameFromItemName()"))
    fun getRawInternalName(itemName: String): String {
        return getInternalNameFromItemName(itemName).asString()
    }

    fun getInternalNameFromItemName(itemName: String): NEUInternalName {
        return getInternalNameOrNull(itemName) ?: throw Error("Internal name is null for '$itemName'")
    }

    fun getInternalNameOrNullIgnoreCase(itemName: String): NEUInternalName? {
        val lowercase = itemName.removeColor().lowercase()
        if (itemNameCache.containsKey(lowercase)) {
            return itemNameCache[lowercase]!!
        }

        if (allItemsCache.isEmpty()) {
            allItemsCache = readAllNeuItems()
        }
        allItemsCache[lowercase]?.let {
            itemNameCache[lowercase] = it
            return it
        }

        return null
    }

    fun readAllNeuItems(): Map<String, NEUInternalName> {
        allInternalNames.clear()
        val map = mutableMapOf<String, NEUInternalName>()
        for (rawInternalName in manager.itemInformation.keys) {
            val name = manager.createItem(rawInternalName).displayName.removeColor().lowercase()
            val internalName = rawInternalName.asInternalName()
            map[name] = internalName
            allInternalNames.add(internalName)
        }
        return map
    }

    fun getInternalNameOrNull(itemName: String): NEUInternalName? {
        val lowercase = itemName.lowercase()
        if (itemNameCache.containsKey(lowercase)) {
            return itemNameCache[lowercase]!!
        }

        if (itemName == "§cmissing repo item") {
            itemNameCache[lowercase] = NEUInternalName.MISSING_ITEM
            return NEUInternalName.MISSING_ITEM
        }

        resolveEnchantmentByName(itemName)?.let {
            val enchantmentName = fixEnchantmentName(it)
            itemNameCache[itemName] = enchantmentName
            return enchantmentName
        }
        var rawInternalName = ItemResolutionQuery.findInternalNameByDisplayName(itemName, false) ?: return null

        // This fixes a NEU bug with §9Hay Bale (cosmetic item)
        // TODO remove workaround when this is fixed in neu
        rawInternalName = if (rawInternalName == "HAY_BALE") "HAY_BLOCK" else rawInternalName

        val internalName = rawInternalName.asInternalName()

        itemNameCache[lowercase] = internalName
        return internalName
    }

    // Workaround for duplex
    private val duplexPattern = "ULTIMATE_DUPLEX;(?<tier>.*)".toPattern()

    private fun fixEnchantmentName(originalName: String): NEUInternalName {
        duplexPattern.matchMatcher(originalName) {
            val tier = group("tier")
            return "ULTIMATE_REITERATE;$tier".asInternalName()
        }
        // TODO USE SH-REPO
        return originalName.asInternalName()
    }

    private fun turboCheck(text: String): String {
        if (text == "Turbo-Cocoa") return "Turbo-Coco"
        if (text == "Turbo-Cacti") return "Turbo-Cactus"
        return text
    }

    fun getInternalName(itemStack: ItemStack) = ItemResolutionQuery(manager)
        .withCurrentGuiContext()
        .withItemStack(itemStack)
        .resolveInternalName()

    fun getInternalNameOrNull(nbt: NBTTagCompound) =
        ItemResolutionQuery(manager).withItemNBT(nbt).resolveInternalName()?.asInternalName()

    fun NEUInternalName.getPrice(useSellingPrice: Boolean = false) = getPriceOrNull(useSellingPrice) ?: -1.0

    fun NEUInternalName.getNpcPrice() = getNpcPriceOrNull() ?: -1.0

    fun NEUInternalName.getNpcPriceOrNull() = BazaarDataHolder.getNpcPrice(this)

    fun transHypixelNameToInternalName(hypixelId: String) =
        manager.auctionManager.transformHypixelBazaarToNEUItemId(hypixelId).asInternalName()

    fun NEUInternalName.getPriceOrNull(useSellingPrice: Boolean = false): Double? {
        if (equals("WISP_POTION")) {
            return 20_000.0
        }
        val result = manager.auctionManager.getBazaarOrBin(asString(), useSellingPrice)
        if (result != -1.0) return result

        if (equals("JACK_O_LANTERN")) {
            return getPrice("PUMPKIN", useSellingPrice) + 1
        }
        if (equals("GOLDEN_CARROT")) {
            // 6.8 for some players
            return 7.0 // NPC price
        }
        return getNpcPriceOrNull()
    }

    fun getPrice(internalName: String, useSellingPrice: Boolean = false) =
        internalName.asInternalName().getPrice(useSellingPrice)

    fun NEUInternalName.getItemStackOrNull() = ItemResolutionQuery(manager)
        .withKnownInternalName(asString())
        .resolveToItemStack()?.copy()

    fun getItemStackOrNull(internalName: String) = internalName.asInternalName().getItemStackOrNull()

    // TODO remove
    @Deprecated("Use NEUInternalName rather than String", ReplaceWith("getItemStack()"))
    fun getItemStack(internalName: String): ItemStack =
        internalName.asInternalName().getItemStack()

    fun NEUInternalName.getItemStack(): ItemStack =
        getItemStackOrNull() ?: run {
            if (getPriceOrNull() == null) return@run fallbackItem
            ErrorManager.logError(
                IllegalStateException("Something went wrong!"),
                "Encountered an error getting the item for §7$this§c. " +
                        "This may be because your NEU repo is outdated. Please ask in the SkyHanni " +
                        "Discord if this is the case"
            )
            fallbackItem
        }

    fun isVanillaItem(item: ItemStack) = manager.auctionManager.isVanillaItem(item.getInternalName().asString())

    fun ItemStack.renderOnScreen(x: Float, y: Float, scaleMultiplier: Double = 1.0) {
        val item = checkBlinkItem()
        val isSkull = item.item === Items.skull

        val baseScale = (if (isSkull) 0.8f else 0.6f)
        val finalScale = baseScale * scaleMultiplier
        val diff = ((finalScale - baseScale) * 10).toFloat()

        val translateX: Float
        val translateY: Float
        if (isSkull) {
            translateX = x - 2 - diff
            translateY = y - 2 - diff
        } else {
            translateX = x - diff
            translateY = y - diff
        }

        GlStateManager.pushMatrix()

        GlStateManager.translate(translateX, translateY, 1F)
        GlStateManager.scale(finalScale, finalScale, 1.0)

        RenderHelper.enableGUIStandardItemLighting()
        Minecraft.getMinecraft().renderItem.renderItemIntoGUI(item, 0, 0)
        RenderHelper.disableStandardItemLighting()

        GlStateManager.popMatrix()
    }

    fun getMultiplier(internalName: NEUInternalName, tryCount: Int = 0): Pair<NEUInternalName, Int> {
        val pair = getMultiplier(internalName.asString(), tryCount)
        return Pair(pair.first.asInternalName(), pair.second)
    }

    fun getMultiplier(internalName: String, tryCount: Int = 0): Pair<String, Int> {
        if (multiplierCache.contains(internalName)) {
            return multiplierCache[internalName]!!
        }
        if (tryCount == 10) {
            val message = "Error reading getMultiplier for item '$internalName'"
            Error(message).printStackTrace()
            LorenzUtils.error(message)
            return Pair(internalName, 1)
        }
        for (recipe in getRecipes(internalName)) {
            if (recipe !is CraftingRecipe) continue

            val map = mutableMapOf<String, Int>()
            for (ingredient in recipe.ingredients) {
                val count = ingredient.count.toInt()
                var internalItemId = ingredient.internalItemId
                // ignore cactus green
                if (internalName == "ENCHANTED_CACTUS_GREEN" && internalItemId == "INK_SACK-2") {
                    internalItemId = "CACTUS"
                }

                // ignore wheat in enchanted cookie
                if (internalName == "ENCHANTED_COOKIE" && internalItemId == "WHEAT") {
                    continue
                }

                // ignore golden carrot in enchanted golden carrot
                if (internalName == "ENCHANTED_GOLDEN_CARROT" && internalItemId == "GOLDEN_CARROT") {
                    continue
                }

                // ignore rabbit hide in leather
                if (internalName == "LEATHER" && internalItemId == "RABBIT_HIDE") {
                    continue
                }

                val old = map.getOrDefault(internalItemId, 0)
                map[internalItemId] = old + count
            }
            if (map.size != 1) continue
            val current = map.iterator().next().toPair()
            val id = current.first
            return if (current.second > 1) {
                val child = getMultiplier(id, tryCount + 1)
                val result = Pair(child.first, child.second * current.second)
                multiplierCache[internalName] = result
                result
            } else {
                Pair(internalName, 1)
            }
        }

        val result = Pair(internalName, 1)
        multiplierCache[internalName] = result
        return result
    }

    fun getRecipes(minionId: String): Set<NeuRecipe> {
        if (recipesCache.contains(minionId)) {
            return recipesCache[minionId]!!
        }
        val recipes = manager.getRecipesFor(minionId)
        recipesCache[minionId] = recipes
        return recipes
    }

    fun neuHasFocus(): Boolean {
        if (AuctionSearchOverlay.shouldReplace()) return true
        if (BazaarSearchOverlay.shouldReplace()) return true
        if (InventoryUtils.inStorage() && InventoryUtils.isNeuStorageEnabled.getValue()) return true
        if (NEUOverlay.searchBarHasFocus) return true

        return false
    }

    // Taken and edited from NEU
    private fun resolveEnchantmentByName(enchantmentName: String) =
        enchantmentNamePattern.matchMatcher(enchantmentName) {
            val name = group("name").trim { it <= ' ' }
            val ultimate = group("format").lowercase().contains("§l")
            ((if (ultimate && name != "Ultimate Wise") "ULTIMATE_" else "")
                    + turboCheck(name).replace(" ", "_").replace("-", "_").uppercase()
                    + ";" + group("level").romanToDecimal())
        }

    //Uses NEU
    fun saveNBTData(item: ItemStack, removeLore: Boolean = true): String {
        val jsonObject = manager.getJsonForItem(item)
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
        return manager.jsonToStack(jsonObject, false)
    }
}
