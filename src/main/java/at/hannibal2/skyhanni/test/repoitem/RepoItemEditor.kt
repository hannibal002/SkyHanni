package at.hannibal2.skyhanni.test.repoitem

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.features.dev.RepoItemEditorConfig
import at.hannibal2.skyhanni.data.repo.RepoManager
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.PrimitiveIngredient.Companion.toPrimitiveIngredient
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.slotUnderCursor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@SkyHanniModule
object RepoItemEditor {

    private val config get(): RepoItemEditorConfig = SkyHanniMod.feature.dev.devTool.repoItemEditor

    private val patternGroup = RepoPattern.group("dev.repoitemeditor")

    private val craftingTablePattern by patternGroup.pattern(
        "craftingtable",
        "§aCrafting Table",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onKeybind(event: GuiKeyPressEvent) {
        if (!config.editModeEnabled) return
        when {
            config.openRepoItemEditorKeybind.isKeyHeld() -> attemptToOpenInEditor(instantSave = false)
            config.instantEditKeybind.isKeyHeld() -> attemptToOpenInEditor(instantSave = true)
            config.saveRecipeKeybind.isKeyHeld() -> saveRecipe()
            config.loadInventoryAsTradesKeybind.isKeyHeld() -> NpcShopExporter.processCurrentlyOpenInventory()
            config.refreshNbtKeybind.isKeyHeld() -> {
                val focussedSlot = slotUnderCursor() ?: return
                val stack = focussedSlot.stack?.copy() ?: return
                val internalName = stack.getInternalNameOrNull() ?: ErrorManager.skyHanniError(
                    "Cannot refresh NBT for item with unknown item name",
                    "displayName" to stack.displayName,
                    "inventoryName" to InventoryUtils.openInventoryName(),
                )
                refreshNbt(internalName)
            }

            else -> return
        }
    }

    private fun attemptToOpenInEditor(instantSave: Boolean) {
        val focussedSlot = slotUnderCursor() ?: return
        val stack = focussedSlot.stack?.copy() ?: return
        stack.openInEditor(instantSave)
    }

    private fun ItemStack.openInEditor(instantSave: Boolean, message: Boolean = true, internalNameOverride: NeuInternalName? = null) {
        val internalName = internalNameOverride ?: getInternalNameOrNull() ?: ErrorManager.skyHanniError(
            "Cannot open item editor for item with unknown item name",
            "displayName" to displayName,
            "inventoryName" to InventoryUtils.openInventoryName(),
        )
        val screen = RepoItemEditorGui(internalName, this)
        if (instantSave) {
            screen.adjustLore()
            screen.saveItem(message)
        } else {
            SkyHanniMod.screenToOpen = screen
        }
    }

    fun openItemInEditor(
        stack: ItemStack,
        instantSave: Boolean = false,
        message: Boolean = true,
        internalNameOverride: NeuInternalName? = null,
    ) {
        stack.openInEditor(instantSave, message, internalNameOverride)
    }

    fun createRepoItemJson(
        baseJson: JsonObject,
        internalName: String,
        minecraftItemId: String,
        displayName: String,
        itemModel: String,
        lore: String,
        craftText: String,
        infoType: String,
        additionalInfo: String,
        clickCommand: String,
        damage: Int,
        nbtTag: NBTTagCompound,
    ): JsonObject {
        baseJson.addProperty("itemid", minecraftItemId)
        baseJson.addProperty("displayname", displayName)
        if (itemModel.isNotEmpty()) {
            baseJson.addProperty("itemModel", itemModel)
        }
        val fixedNbtTagString = nbtTag.toString()
            //#if MC > 1.21
            //$$ .replace("\u0027[", "[")
            //$$ .replace("]\u0027", "]")
            //$$ .replace("\\\u0027", "\u0027")
            //$$ .replace("\\\\", "\\")
        //#endif

        baseJson.addProperty("nbttag", fixedNbtTagString)
        baseJson.addProperty("damage", damage)
        val jsonLore = JsonArray()
        lore.split("\n").forEach { line ->
            jsonLore.add(JsonPrimitive(line))
        }
        baseJson.add("lore", jsonLore)
        baseJson.addProperty("internalname", internalName)
        baseJson.addProperty("crafttext", craftText)
        baseJson.addProperty("clickcommand", clickCommand)
        baseJson.addProperty("modver", SkyHanniMod.VERSION)
        baseJson.addProperty("infoType", infoType)
        if (additionalInfo.isNotEmpty()) {
            val additionalInfoArray = JsonArray()
            additionalInfo.split("\n").forEach { line ->
                additionalInfoArray.add(JsonPrimitive(line))
            }
            baseJson.add("info", additionalInfoArray)
        } else {
            baseJson.add("info", JsonArray())
        }
        return baseJson
    }

    //#if MC > 1.21
    //$$ fun convertStringTagListToString(list: net.minecraft.nbt.NbtList): String {
    //$$     return buildString {
    //$$         append("[")
    //$$         for ((index, tag) in list.value.withIndex()) {
    //$$             if (index != 0) {
    //$$                 append(",")
    //$$             }
    //$$             append("$index:")
    //$$             var toString = tag.toString()
    //$$             if (toString.startsWith("\u0027")) {
    //$$                 toString = toString.removePrefix("\u0027").removeSuffix("\u0027")
    //$$                 toString = toString.replace("\"", "\\\"")
    //$$                 toString = "\"$toString\""
    //$$             }
    //$$             append(toString)
    //$$         }
    //$$         append("]")
    //$$     }
    //$$ }
    //#endif

    fun saveItemToRepo(internalName: NeuInternalName, json: JsonObject) {
        val itemsDir = File(EnoughUpdatesManager.repoLocation, "items")
        val itemFile = File(itemsDir, "${internalName.asString()}.json")
        RepoManager.writeJson(json, itemFile)
        val firmFolder = File(".firmament/repo-extracted/items")
        if (firmFolder.exists()) {
            val firmItemFile = File(firmFolder, "${internalName.asString()}.json")
            RepoManager.writeJson(json, firmItemFile)
        }

        val skyblockerFolder = File("config/skyblocker/item-repo/items")
        if (skyblockerFolder.exists()) {
            val skyblockerItemFile = File(skyblockerFolder, "${internalName.asString()}.json")
            RepoManager.writeJson(json, skyblockerItemFile)
        }

        EnoughUpdatesManager.reloadDataForItem(internalName, json)
    }

    private fun saveRecipe() {
        val craftingSlot = InventoryUtils.getSlotAtIndex(23) ?: return
        val craftingStack = craftingSlot.stack.orNull() ?: return
        if (!craftingTablePattern.matches(craftingStack.displayName)) return

        val resultItem = InventoryUtils.getSlotAtIndex(25)?.stack?.orNull() ?: return
        val resultInternalName = resultItem.getInternalNameOrNull() ?: ErrorManager.skyHanniError(
            "Cannot save recipe for item with unknown item name",
            "displayName" to resultItem.displayName,
            "inventoryName" to InventoryUtils.openInventoryName(),
        )
        if (!NeuItems.allNeuRepoItems().containsKey(resultInternalName.asString())) {
            resultItem.openInEditor(instantSave = true)
        }
        val existingJson = EnoughUpdatesManager.getItemById(resultInternalName.asString()) ?: ErrorManager.skyHanniError(
            "Still no item data saved for: $resultInternalName",
        )

        val recipeJson = JsonObject()
        for (j in 0..2) {
            for (i in 0..2) {
                val slotIndex = j * 9 + i + 10
                val slotStack = InventoryUtils.getSlotAtIndex(slotIndex)?.stack?.orNull()

                val recipeTileName = "${'A' + j}${i + 1}"
                println("slotIndex: $slotIndex, tileName: $recipeTileName")
                recipeJson.addProperty(recipeTileName, slotStack.toPrimitiveIngredient().asRepoString())
            }
        }
        existingJson.add("recipe", recipeJson)
        existingJson.addProperty("clickcommand", "viewrecipe")
        existingJson.addProperty("modver", SkyHanniMod.VERSION)

        saveItemToRepo(resultInternalName, existingJson)
        ChatUtils.chat("§aSuccessfully saved recipe for ${resultInternalName.asString()} to repo folder!")
    }

    private fun refreshNbt(internalName: NeuInternalName) {
        val file = File(EnoughUpdatesManager.repoLocation, "items/${internalName.asString()}.json")
        if (!file.exists()) {
            ChatUtils.chat("§cCould not find item with internal name §e${internalName.asString()}§c.")
            return
        }
        try {
            InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8).use { reader ->
                val json = ConfigManager.gson.fromJson(reader, JsonObject::class.java)
                val stack = EnoughUpdatesManager.jsonToStack(json, useCache = false)
                stack.openInEditor(instantSave = true, message = false)
                ChatUtils.chat("§aSuccessfully refreshed NBT for item §e${internalName.asString()}§a.")
            }

        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e, "Error refreshing NBT for item",
                "internalName" to internalName.asString(),
                ignoreErrorCache = true,
            )
        }
    }

    // TODO: Also use Empa's pr for tab completion of this
    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("sheditrepoitem") {
            description = "Open the RepoItemEditor for the specified internal name"
            category = CommandCategory.DEVELOPER_TEST
            argCallback("internalName", BrigadierArguments.greedyString()) { internalName ->
                val item = internalName.toInternalName().getItemStackOrNull()
                if (item == null) {
                    ChatUtils.chat("§cCould not find item with internal name §e$internalName§c.")
                    return@argCallback
                }
                item.openInEditor(instantSave = false)
            }
        }
        event.registerBrigadier("shrefreshrepoitem") {
            description = "Refresh the repo item so that the nbt matches the rest of the data"
            category = CommandCategory.DEVELOPER_TEST
            argCallback("internalName", BrigadierArguments.greedyString()) { internalName ->
                refreshNbt(internalName.toInternalName())
            }
        }
    }
}
