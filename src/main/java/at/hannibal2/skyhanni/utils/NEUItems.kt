package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.features.bazaar.BazaarDataHolder
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemBlink.checkBlinkItem
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.JsonObject
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import java.util.regex.Pattern

object NEUItems {
    //    val manager: NEUManager get() = NotEnoughUpdates.INSTANCE.manager
    private val itemNameCache = mutableMapOf<String, NEUInternalName>() // item name -> internal name
    private val multiplierCache = mutableMapOf<NEUInternalName, Pair<NEUInternalName, Int>>()

    //    private val recipesCache = mutableMapOf<NEUInternalName, Set<NeuRecipe>>()
//    private val ingredientsCache = mutableMapOf<NeuRecipe, Set<Ingredient>>()
    private val enchantmentNamePattern = Pattern.compile("^(?<format>(?:§.)+)(?<name>[^§]+) (?<level>[IVXL]+)$")
    var allItemsCache = mapOf<String, NEUInternalName>() // item name -> internal name
    var allInternalNames = mutableListOf<NEUInternalName>()

    private val fallbackItem by lazy {
        createItemStack(
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
//        for (rawInternalName in allNeuRepoItems().keys) {
//            val name = manager.createItem(rawInternalName).displayName.removeColor().lowercase()
//            val internalName = rawInternalName.asInternalName()
//            map[name] = internalName
//            allInternalNames.add(internalName)
//        }
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
//        var rawInternalName = ItemResolutionQuery.findInternalNameByDisplayName(itemName, false) ?: return null
//
//        // This fixes a NEU bug with §9Hay Bale (cosmetic item)
//        // TODO remove workaround when this is fixed in neu
//        rawInternalName = if (rawInternalName == "HAY_BALE") "HAY_BLOCK" else rawInternalName
//
//        val internalName = rawInternalName.asInternalName()
//
//        itemNameCache[lowercase] = internalName
//        return internalName
        return null
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

    fun getInternalName(itemStack: ItemStack): String? = null
//    fun getInternalName(itemStack: ItemStack): String? = ItemResolutionQuery(manager)
//        .withCurrentGuiContext()
//        .withItemStack(itemStack)
//        .resolveInternalName()

    fun getInternalNameOrNull(nbt: NBTTagCompound): NEUInternalName? = null
//    fun getInternalNameOrNull(nbt: NBTTagCompound): NEUInternalName? =
//        ItemResolutionQuery(manager).withItemNBT(nbt).resolveInternalName()?.asInternalName()

    fun NEUInternalName.getPrice(useSellingPrice: Boolean = false) = getPriceOrNull(useSellingPrice) ?: -1.0

    fun NEUInternalName.getNpcPrice() = getNpcPriceOrNull() ?: -1.0

    fun NEUInternalName.getNpcPriceOrNull(): Double? {
        if (equals("WISP_POTION")) {
            return 20_000.0
        }
        return BazaarDataHolder.getNpcPrice(this)
    }

    fun transHypixelNameToInternalName(hypixelId: String): NEUInternalName =
        hypixelId.uppercase().asInternalName()
//         manager.auctionManager.transformHypixelBazaarToNEUItemId(hypixelId).asInternalName()

    fun NEUInternalName.getPriceOrNull(useSellingPrice: Boolean = false): Double? {
        if (equals("WISP_POTION")) {
            return 20_000.0
        }
//        val result = manager.auctionManager.getBazaarOrBin(asString(), useSellingPrice)
        val result = -1.0
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

    fun getPrice(internalName: String, useSellingPrice: Boolean = false): Double =
        internalName.asInternalName().getPrice(useSellingPrice)

    fun NEUInternalName.getItemStackOrNull(): ItemStack? = null
//    fun NEUInternalName.getItemStackOrNull(): ItemStack? = ItemResolutionQuery(manager)
//        .withKnownInternalName(asString())
//        .resolveToItemStack()?.copy()

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

       fun isVanillaItem(item: ItemStack): Boolean = false
//         manager.auctionManager.isVanillaItem(item.getInternalName().asString())
//     fun isVanillaItem(item: ItemStack) = false

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

    fun allNeuRepoItems(): Map<String, JsonObject> = emptyMap()

    fun getMultiplier(internalName: NEUInternalName, tryCount: Int = 0): Pair<NEUInternalName, Int> {
        if (multiplierCache.contains(internalName)) {
            return multiplierCache[internalName]!!
        }
        if (tryCount == 10) {
            ErrorManager.logErrorStateWithData(
                "Cound not load recipe data.",
                "Failed to find item multiplier",
                "internalName" to internalName
            )
            return Pair(internalName, 1)
        }
//        for (recipe in getRecipes(internalName)) {
//            if (recipe !is CraftingRecipe) continue
//
//            val map = mutableMapOf<NEUInternalName, Int>()
//            for (ingredient in recipe.getCachedIngredients()) {
//                val count = ingredient.count.toInt()
//                var internalItemId = ingredient.internalItemId.asInternalName()
//                // ignore cactus green
//                if (internalName == "ENCHANTED_CACTUS_GREEN".asInternalName() && internalItemId == "INK_SACK-2".asInternalName()) {
//                    internalItemId = "CACTUS".asInternalName()
//                }
//
//                // ignore wheat in enchanted cookie
//                if (internalName == "ENCHANTED_COOKIE".asInternalName() && internalItemId == "WHEAT".asInternalName()) {
//                    continue
//                }
//
//                // ignore golden carrot in enchanted golden carrot
//                if (internalName == "ENCHANTED_GOLDEN_CARROT".asInternalName() && internalItemId == "GOLDEN_CARROT".asInternalName()) {
//                    continue
//                }
//
//                // ignore rabbit hide in leather
//                if (internalName == "LEATHER".asInternalName() && internalItemId == "RABBIT_HIDE".asInternalName()) {
//                    continue
//                }
//
//                val old = map.getOrDefault(internalItemId, 0)
//                map[internalItemId] = old + count
//            }
//            if (map.size != 1) continue
//            val current = map.iterator().next().toPair()
//            val id = current.first
//            return if (current.second > 1) {
//                val child = getMultiplier(id, tryCount + 1)
//                val result = Pair(child.first, child.second * current.second)
//                multiplierCache[internalName] = result
//                result
//            } else {
//                Pair(internalName, 1)
//            }
//        }

        val result = Pair(internalName, 1)
        multiplierCache[internalName] = result
        return result

    }

    @Deprecated("Do not use strings as id", ReplaceWith("getMultiplier with NEUInternalName"))
    fun getMultiplier(internalName: String, tryCount: Int = 0): Pair<String, Int> {
        val pair = getMultiplier(internalName.asInternalName(), tryCount)
        return Pair(pair.first.asString(), pair.second)
    }

//    fun getRecipes(internalName: NEUInternalName): Set<NeuRecipe> {
//        if (recipesCache.contains(internalName)) {
//            return recipesCache[internalName]!!
//        }
//        val recipes = manager.getRecipesFor(internalName.asString())
//        recipesCache[internalName] = recipes
//        return recipes
//    }

//    fun NeuRecipe.getCachedIngredients() = ingredientsCache.getOrPut(this) { ingredients }

    fun neuHasFocus(): Boolean {
//        if (AuctionSearchOverlay.shouldReplace()) return true
//        if (BazaarSearchOverlay.shouldReplace()) return true
//        if (InventoryUtils.inStorage() && InventoryUtils.isNeuStorageEnabled.getValue()) return true
//        if (NEUOverlay.searchBarHasFocus) return true

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
        return "{abc}"
//        val jsonObject = manager.getJsonForItem(item)
//        if (!jsonObject.has("internalname")) {
//            jsonObject.add("internalname", JsonPrimitive("_"))
//        }
//        if (removeLore && jsonObject.has("lore")) jsonObject.remove("lore")
//        val jsonString = jsonObject.toString()
//        return StringUtils.encodeBase64(jsonString)
    }

    fun loadNBTData(encoded: String): ItemStack {
//        val jsonString = StringUtils.decodeBase64(encoded)
//        val jsonObject = ConfigManager.gson.fromJson(jsonString, JsonObject::class.java)
//        return manager.jsonToStack(jsonObject, false)
        return ItemStack(Items.wooden_hoe)
    }

    fun createItemStack(item: Item?, displayName: String?, vararg lore: String?): ItemStack {
        return createItemStack(item, displayName, 0, *lore)
    }

    fun createItemStackArray(item: Item?, displayName: String?, lore: Array<String?>): ItemStack {
        return createItemStack(item, displayName, 0, *lore)
    }

    fun createItemStack(item: Block?, displayName: String?, vararg lore: String?): ItemStack {
        return createItemStack(Item.getItemFromBlock(item), displayName, *lore)
    }

    fun createItemStack(item: Item?, displayName: String?, damage: Int, vararg lore: String?): ItemStack {
        val stack = ItemStack(item, 1, damage)
        val tag = NBTTagCompound()
        addNameAndLore(tag, displayName, *lore)
        tag.setInteger("HideFlags", 254)
        stack.tagCompound = tag
        return stack
    }

    private fun addNameAndLore(tag: NBTTagCompound, displayName: String?, vararg lore: String?) {
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

    fun editItemStackInfo(
        itemStack: ItemStack,
        displayName: String?,
        disableNeuToolTips: Boolean,
        vararg lore: String?
    ): ItemStack {
        var tag = itemStack.tagCompound
        if (tag == null) tag = NBTTagCompound()
        val display = tag.getCompoundTag("display")
        val Lore = NBTTagList()
        for (line in lore) {
            Lore.appendTag(NBTTagString(line))
        }
        display.setString("Name", displayName)
        display.setTag("Lore", Lore)
        tag.setTag("display", display)
        tag.setInteger("HideFlags", 254)
        if (disableNeuToolTips) {
            tag.setBoolean("disableNeuTooltip", true)
        }
        itemStack.tagCompound = tag
        return itemStack
    }
}
