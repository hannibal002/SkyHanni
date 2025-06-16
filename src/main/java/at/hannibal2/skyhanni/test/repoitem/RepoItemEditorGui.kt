package at.hannibal2.skyhanni.test.repoitem

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.extraAttributes
import at.hannibal2.skyhanni.utils.ItemUtils.findItemDamage
import at.hannibal2.skyhanni.utils.ItemUtils.getItemModel
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.hasEnchGlint
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.UtilsPatterns
import at.hannibal2.skyhanni.utils.compat.NbtCompat.appendString
import at.hannibal2.skyhanni.utils.compat.SkyhanniBaseScreen
import at.hannibal2.skyhanni.utils.compat.getIdentifierString
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagList
//#if MC > 1.21
//$$ import at.hannibal2.skyhanni.utils.ComponentUtils
//$$ import net.minecraft.component.MergedComponentMap
//$$ import net.minecraft.component.DataComponentTypes
//$$ import net.minecraft.component.type.NbtComponent
//$$ import net.minecraft.component.type.ProfileComponent
//$$ import net.minecraft.nbt.NbtCompound
//#endif

class RepoItemEditorGui(internalName: NeuInternalName, underlyingStack: ItemStack) : SkyhanniBaseScreen() {

    private val baseJson = EnoughUpdatesManager.getItemById(internalName.asString()) ?: JsonObject()
    private var internalNameString = internalName.asString()
    private var displayName = underlyingStack.displayName
    private var minecraftItemId: String
    private var itemModel: String
    private var lore = underlyingStack.getLore().joinToString("\n")
    private var craftText: String
    private var infoType: String
    private var additionalInfo: String
    private var clickCommand: String
    private var damage: String
    private var hasEnchantGlint = underlyingStack.hasEnchGlint()

    private var nbtTag = underlyingStack.tagCompound
    //#if MC > 1.21
    //$$ as MergedComponentMap
    //#endif

    init {

        val baseUnderlyingMinecraftId = underlyingStack.item.getIdentifierString()
        val modernItemModel = underlyingStack.getItemModel()?.getIdentifierString().orEmpty()
        itemModel = if (modernItemModel != baseUnderlyingMinecraftId) modernItemModel else ""

        val extraAttributes = nbtTag.extraAttributes
        extraAttributes.removeTag("uuid")
        extraAttributes.removeTag("timestamp")

        if (extraAttributes.hasKey("petInfo")) {
            val petInfo = extraAttributes.getString("petInfo")
            val petInfoJson = ConfigManager.gson.fromJson<JsonObject>(petInfo)
            petInfoJson.remove("heldItem")
            petInfoJson.add("exp", JsonPrimitive(0))
            petInfoJson.add("candyUsed", JsonPrimitive(0))
            extraAttributes.setString("petInfo", petInfoJson.toString())
        }
        //#if MC < 1.21
        minecraftItemId = baseUnderlyingMinecraftId
        damage = underlyingStack.findItemDamage().toString()
        nbtTag.setTag("ExtraAttributes", extraAttributes)
        //#else
        //$$ val (id, itemDamage) = ComponentUtils.convertModernToLegacyId(baseUnderlyingMinecraftId)
        //$$ minecraftItemId = id
        //$$ damage = itemDamage.toString()
        //$$ nbtTag.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(extraAttributes))
        //#endif

        craftText = baseJson.get("craftText")?.asString.orEmpty()
        infoType = baseJson.get("infoType")?.asString.orEmpty()
        additionalInfo = baseJson.get("info")?.asJsonArray?.joinToString("\n") { it.asString } ?: ""
        clickCommand = baseJson.get("clickcommand")?.asString.orEmpty()
    }

    fun adjustLore() {
        val loreList = lore.split("\n")
        val newLore = mutableListOf<String>()
        for (line in loreList) {
            if (!UtilsPatterns.rarityLoreLinePattern.matches(line)) {
                newLore.add(line)
            } else {
                newLore.add(line)
                lore = newLore.joinToString("\n")
            }
        }
    }

    fun saveItem(message: Boolean = true) {
        try {
            RepoItemEditor.saveItemToRepo(internalNameString.toInternalName(), createRepoItemJson())
            if (message) {
                ChatUtils.chat("§aSuccessfully saved $internalNameString to repo folder!")
            }
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Failed to save $internalNameString to repo folder!", ignoreErrorCache = true)
        }
    }

    private fun createRepoItemJson(): JsonObject {
        val damageInt = damage.toIntOrNull() ?: 0
        val json = baseJson
        json.addProperty("itemid", minecraftItemId)
        json.addProperty("displayname", displayName)
        //#if MC < 1.21
        nbtTag.setInteger("HideFlags", 254)
        if (hasEnchantGlint) {
            nbtTag.setTag("ench", NBTTagList())
        } else {
            nbtTag.removeTag("ench")
        }

        val loreList = NBTTagList()
        lore.split("\n").forEach { line ->
            loreList.appendString(line)
        }
        val display = nbtTag.getCompoundTag("display")
        display.setTag("Lore", loreList)
        display.setString("Name", displayName)
        nbtTag.setTag("display", display)

        val extraAttributes = nbtTag.extraAttributes
        extraAttributes.setString("id", internalNameString)
        nbtTag.setTag("ExtraAttributes", extraAttributes)

        json.addProperty("nbttag", nbtTag.toString())

        //#else
        //$$ val nbt = getModernNbtTag(nbtTag.get(DataComponentTypes.PROFILE), nbtTag.extraAttributes)
        //$$ json.addProperty("nbttag", nbt.toString())
        //#endif

        json.addProperty("damage", damageInt)

        val jsonLore = JsonArray()
        lore.split("\n").forEach { line ->
            jsonLore.add(JsonPrimitive(line))
        }
        json.add("lore", jsonLore)

        if (itemModel.isNotEmpty()) {
            json.addProperty("itemModel", itemModel)
        }

        json.addProperty("internalname", internalNameString)
        json.addProperty("crafttext", craftText)
        json.addProperty("clickcommand", clickCommand)
        json.addProperty("modver", SkyHanniMod.VERSION)
        json.addProperty("infoType", infoType)
        if (additionalInfo.isNotEmpty()) {
            val additionalInfoArray = JsonArray()
            additionalInfo.split("\n").forEach { line ->
                additionalInfoArray.add(JsonPrimitive(line))
            }
            json.add("info", additionalInfoArray)
        }

        return json
    }

    //#if MC > 1.21
    //$$ private fun getModernNbtTag(profileComponent: ProfileComponent?, extraAttributes: NbtCompound): NbtCompound {
    //$$     val tag = NbtCompound()
    //$$     tag.putInt("HideFlags", 254)
    //$$     if (hasEnchantGlint) {
    //$$         tag.put("ench", NbtList())
    //$$     }
    //$$
    //$$     profileComponent?.let {
    //$$         val skullOwner = NbtCompound()
    //$$         skullOwner.putString("Id", it.id.get().toString())
    //$$         skullOwner.putBoolean("hypixelPopulated", true)
    //$$         val properties = NbtCompound()
    //$$         val skullTexture = it.properties.get("textures").first()
    //$$         val textures = NbtCompound()
    //$$         if (skullTexture.hasSignature()) {
    //$$             textures.putString("Signature", skullTexture.signature)
    //$$         }
    //$$         textures.putString("Value", skullTexture.value)
    //$$
    //$$         properties.put(
    //$$             "textures",
    //$$             NbtList().apply {
    //$$                 add(textures)
    //$$             },
    //$$         )
    //$$         skullOwner.put("Properties", properties)
    //$$         tag.put("SkullOwner", skullOwner)
    //$$     }
    //$$
    //$$     if (nbtTag.contains(DataComponentTypes.UNBREAKABLE)) {
    //$$         tag.putBoolean("Unbreakable", true)
    //$$     }
    //$$
    //$$     if (nbtTag.get(DataComponentTypes.ATTRIBUTE_MODIFIERS)?.modifiers?.isNotEmpty() == true) {
    //$$         println("modifiers: ${nbtTag.get(DataComponentTypes.ATTRIBUTE_MODIFIERS)}")
    //$$         tag.putBoolean("overrideMeta", true)
    //$$         tag.put("AttributeModifiers", NbtList())
    //$$     }
    //$$
    //$$     val loreList = NbtList()
    //$$     lore.split("\n").forEach { line ->
    //$$         loreList.appendString(line)
    //$$     }
    //$$
    //$$     val display = NbtCompound()
    //$$     display.put("Lore", loreList)
    //$$     display.putString("Name", displayName)
    //$$     val color = nbtTag.get(DataComponentTypes.DYED_COLOR)?.rgb
    //$$     color?.let {
    //$$         display.putInt("color", it)
    //$$     }
    //$$     tag.put("display", display)
    //$$
    //$$     extraAttributes.putString("id", internalNameString)
    //$$     tag.put("ExtraAttributes", extraAttributes)
    //$$
    //$$     return tag
    //$$ }
    //#endif
}
