package at.hannibal2.skyhanni.test.repoitem

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
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
//#if MC > 1.21
//$$ import at.hannibal2.skyhanni.utils.ComponentUtils
//$$ import net.minecraft.component.MergedComponentMap
//$$ import net.minecraft.component.DataComponentTypes
//$$ import net.minecraft.component.type.NbtComponent
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
        additionalInfo = baseJson.get("info")?.asJsonArray?.joinToString("\n") { it.asString }.orEmpty()
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
            val newJson = RepoItemEditor.createRepoItemJson(
                baseJson,
                internalNameString,
                minecraftItemId,
                displayName,
                itemModel,
                lore,
                craftText,
                infoType,
                additionalInfo,
                clickCommand,
                damage.toIntOrNull() ?: 0,
                getNbtTag()
            )
            RepoItemEditor.saveItemToRepo(internalNameString.toInternalName(), newJson)
            if (message) {
                ChatUtils.chat("§aSuccessfully saved $internalNameString to repo folder!")
            }
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Failed to save $internalNameString to repo folder!", ignoreErrorCache = true)
        }
    }

    private fun getNbtTag(): NBTTagCompound {
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
        return nbtTag
        //#else
        //$$     val tag = NbtCompound()
        //$$     tag.putInt("HideFlags", 254)
        //$$     if (hasEnchantGlint) {
        //$$         tag.put("ench", NbtList())
        //$$     }
        //$$
        //$$     nbtTag.get(DataComponentTypes.PROFILE)?.let {
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
        //$$     val extraAttributes = nbtTag.extraAttributes
        //$$     extraAttributes.putString("id", internalNameString)
        //$$     tag.put("ExtraAttributes", extraAttributes)
        //$$
        //$$     return tag
        //#endif
    }
}
