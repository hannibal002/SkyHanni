package at.hannibal2.skyhanni.test.repoitem

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.extraAttributes
import at.hannibal2.skyhanni.utils.ItemUtils.findItemDamage
import at.hannibal2.skyhanni.utils.ItemUtils.getItemModel
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.hasEnchGlint
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.UtilsPatterns
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.NbtCompat.appendString
import at.hannibal2.skyhanni.utils.compat.SkyhanniBaseScreen
import at.hannibal2.skyhanni.utils.compat.getIdentifierString
import at.hannibal2.skyhanni.utils.compat.getVanillaItem
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.renderables.TextFieldRenderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import java.awt.Color

//#if MC > 1.21
//$$ import at.hannibal2.skyhanni.utils.ComponentUtils
//$$ import net.minecraft.component.MergedComponentMap
//$$ import net.minecraft.component.DataComponentTypes
//$$ import net.minecraft.component.type.NbtComponent
//#endif

class RepoItemEditorGui(internalName: NeuInternalName, underlyingStack: ItemStack) : SkyhanniBaseScreen() {

    private val baseJson = EnoughUpdatesManager.getItemById(internalName.asString()) ?: JsonObject()
    private var internalNameStringField = TextFieldRenderable(internalName.asString())
    private var displayNameField = TextFieldRenderable(underlyingStack.displayName)
    private var minecraftItemIdField = TextFieldRenderable()
    private var itemModelField = TextFieldRenderable()
    private var loreField = TextFieldRenderable(underlyingStack.getLore().joinToString("\n"))
    private var craftTextField = TextFieldRenderable()
    private var infoTypeField = TextFieldRenderable()
    private var additionalInfoField = TextFieldRenderable()
    private var clickCommandField = TextFieldRenderable()
    private var damageField = TextFieldRenderable()
    private var hasEnchantGlint = underlyingStack.hasEnchGlint()

    private var nbtTag = underlyingStack.tagCompound
    //#if MC > 1.21
    //$$ as MergedComponentMap
    //#endif

    init {

        val baseUnderlyingMinecraftId = underlyingStack.item.getIdentifierString()
        val modernItemModel = underlyingStack.getItemModel()?.getIdentifierString().orEmpty()
        if (modernItemModel != baseUnderlyingMinecraftId) {
            itemModelField.setText(modernItemModel)
        } else {
            itemModelField.setText("")
        }

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
        minecraftItemIdField.setText(baseUnderlyingMinecraftId)
        damageField.setText(underlyingStack.findItemDamage().toString())
        nbtTag.setTag("ExtraAttributes", extraAttributes)
        //#else
        //$$ val (id, itemDamage) = ComponentUtils.convertModernToLegacyId(baseUnderlyingMinecraftId)
        //$$ minecraftItemIdField.setText(id)
        //$$ damageField.setText(itemDamage.toString())
        //$$ nbtTag.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(extraAttributes))
        //#endif

        craftTextField.setText(baseJson.get("craftText")?.asString.orEmpty())
        infoTypeField.setText(baseJson.get("infoType")?.asString.orEmpty())
        additionalInfoField.setText(baseJson.get("info")?.asJsonArray?.joinToString("\n") { it.asString }.orEmpty())
        clickCommandField.setText(baseJson.get("clickcommand")?.asString.orEmpty())
    }

    private val renderableList = listOf(
        RenderableString("Internal Name:"),
        internalNameStringField,
        RenderableString("Item ID:"),
        minecraftItemIdField,
        RenderableString("Display name:"),
        displayNameField,
        RenderableString("Lore:"),
        loreField,
        RenderableString("Item Model:"),
        itemModelField,
        RenderableString("Craft text:"),
        craftTextField,
        RenderableString("Info type:"),
        infoTypeField,
        RenderableString("Additional information:"),
        additionalInfoField,
        RenderableString("Click-command (viewrecipe or viewpotion)"),
        clickCommandField,
        RenderableString("Damage:"),
        damageField,
    )

    private fun getTextFields(): List<TextFieldRenderable> {
        return listOf(
            internalNameStringField,
            minecraftItemIdField,
            displayNameField,
            loreField,
            itemModelField,
            craftTextField,
            infoTypeField,
            additionalInfoField,
            clickCommandField,
            damageField,
        )
    }

    private fun findWhereThingsActuallyAre(offsetX: Int, offsetY: Int) {
        var height = 0
        for (renderable in renderableList) {
            if (renderable is TextFieldRenderable) {
                renderable.setOffset(offsetX, offsetY + height)
            }
            height += renderable.height + 2
        }
    }

    override fun onDrawScreen(originalMouseX: Int, originalMouseY: Int, partialTicks: Float) {
        drawDefaultBackground(originalMouseY, originalMouseX, partialTicks)

        val table = VerticalContainerRenderable(renderableList, spacing = 2)
        DrawContextUtils.translate(15.0, 15.0, 0.0)
        findWhereThingsActuallyAre(15, 15)
        table.render(0, 0)
        DrawContextUtils.translate(-15.0, -15.0, 0.0)

        val damageInt = damageField.getText().toIntOrNull() ?: 0
        val itemToRender = when {
            itemModelField.getText().getVanillaItem() != null -> ItemStack(itemModelField.getText().getVanillaItem())
            minecraftItemIdField.getText().getVanillaItem() != null -> {
                //#if MC < 1.21
                ItemStack(minecraftItemIdField.getText().getVanillaItem(), 1, damageInt)
                //#else
                //$$ ItemStack(minecraftItemIdField.getText().getVanillaItem())
                //#endif
            }

            minecraftItemIdField.getText().isNotEmpty() -> {
                //#if MC < 1.21
                null
                //#else
                //$$ ItemStack(ComponentUtils.convertMinecraftIdToModern(minecraftItemIdField.getText(), damageInt).getVanillaItem())
                //#endif
            }

            else -> null
        }

        DrawContextUtils.pushPop {
            DrawContextUtils.translate(390f, 90f, 0f)
            GuiRenderUtils.drawRect(-10, -10, 90, 90, Color.GRAY.rgb)
            DrawContextUtils.translate(0f, 0f, 0f)
            if (itemToRender != null) {
                //#if MC < 1.21
                itemToRender.tagCompound = nbtTag
                //#else
                //$$ itemToRender.copy(DataComponentTypes.PROFILE, nbtTag)
                //$$ itemToRender.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, hasEnchantGlint)
                //#endif
                Renderable.itemStack(itemToRender, scale = 5.0).render(0, 0)
            }
        }

        DrawContextUtils.translate(700f, 10f, 0f)
        Renderable.clickable("§7Close (discards changes)", onLeftClick = { InventoryUtils.closeInventory() }).render(700, 10)
        DrawContextUtils.translate(0f, 40f, 0f)
        Renderable.clickable("§aSave to local disk", onLeftClick = {}).render(700, 50)
        DrawContextUtils.translate(0f, 20f, 0f)
        Renderable.clickable("§5Remove enchants", onLeftClick = {}).render(700, 70)
        DrawContextUtils.translate(0f, 20f, 0f)
        Renderable.clickable("§6Add enchant glint", onLeftClick = {}).render(700, 90)
        DrawContextUtils.translate(-700f, -90f, 0f)
    }

    override fun onKeyTyped(typedChar: Char?, keyCode: Int?) {
        getTextFields().forEach {
            it.keyTyped(typedChar ?: ' ', keyCode ?: 0)
        }
    }

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        getTextFields().forEach {
            it.mouseClicked(originalMouseX, originalMouseY, mouseButton)
        }
    }

    override fun onMouseClickMove(originalMouseX: Int, originalMouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        getTextFields().forEach {
            it.mouseClickMove(originalMouseX, originalMouseY, clickedMouseButton, timeSinceLastClick)
        }
    }

    fun adjustLore() {
        val loreList = loreField.getText().split("\n")
        val newLore = mutableListOf<String>()
        for (line in loreList) {
            if (!UtilsPatterns.rarityLoreLinePattern.matches(line)) {
                newLore.add(line)
            } else {
                newLore.add(line)
                loreField.setText(newLore.joinToString("\n"))
            }
        }
    }

    fun saveItem(message: Boolean = true) {
        try {
            val newJson = RepoItemEditor.createRepoItemJson(
                baseJson,
                internalNameStringField.getText(),
                minecraftItemIdField.getText(),
                displayNameField.getText(),
                itemModelField.getText(),
                loreField.getText(),
                craftTextField.getText(),
                infoTypeField.getText(),
                additionalInfoField.getText(),
                clickCommandField.getText(),
                damageField.getText().toIntOrNull() ?: 0,
                getNbtTag(),
            )
            RepoItemEditor.saveItemToRepo(internalNameStringField.getText().toInternalName(), newJson)
            if (message) {
                ChatUtils.chat("§aSuccessfully saved $internalNameStringField to repo folder!")
            }
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Failed to save $internalNameStringField to repo folder!", ignoreErrorCache = true)
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
        loreField.getText().split("\n").forEach { line ->
            loreList.appendString(line)
        }
        val display = nbtTag.getCompoundTag("display")
        display.setTag("Lore", loreList)
        display.setString("Name", displayNameField.getText())
        nbtTag.setTag("display", display)

        val extraAttributes = nbtTag.extraAttributes
        extraAttributes.setString("id", internalNameStringField.getText())
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
        //$$     loreField.getText().split("\n").forEach { line ->
        //$$         loreList.appendString(line)
        //$$     }
        //$$
        //$$     val display = NbtCompound()
        //$$     display.put("Lore", loreList)
        //$$     display.putString("Name", displayNameField.getText())
        //$$     val color = nbtTag.get(DataComponentTypes.DYED_COLOR)?.rgb
        //$$     color?.let {
        //$$         display.putInt("color", it)
        //$$     }
        //$$     tag.put("display", display)
        //$$
        //$$     val extraAttributes = nbtTag.extraAttributes
        //$$     extraAttributes.putString("id", internalNameStringField.getText())
        //$$     tag.put("ExtraAttributes", extraAttributes)
        //$$
        //$$     return tag
        //#endif
    }
}
