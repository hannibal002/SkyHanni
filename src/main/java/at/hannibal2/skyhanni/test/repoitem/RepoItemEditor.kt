package at.hannibal2.skyhanni.test.repoitem

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
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

@SkyHanniModule
object RepoItemEditor {

    val config get(): RepoItemEditorConfig = SkyHanniMod.feature.dev.devTool.repoItemEditor

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
            else -> return
        }
    }

    private fun attemptToOpenInEditor(instantSave: Boolean) {
        val focussedSlot = slotUnderCursor() ?: return
        val stack = focussedSlot.stack?.copy() ?: return
        stack.openInEditor(instantSave)
    }

    private fun ItemStack.openInEditor(instantSave: Boolean, message: Boolean = true) {
        val internalName = getInternalNameOrNull() ?: ErrorManager.skyHanniError(
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
        baseJson.addProperty("nbttag", nbtTag.toString())
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
        }
        return baseJson
    }

    fun saveItemToRepo(internalName: NeuInternalName, json: JsonObject) {
        val itemsDir = File(EnoughUpdatesManager.repoLocation, "items")
        val itemFile = File(itemsDir, "${internalName.asString()}.json")
        println("saving item to repo: ${itemFile.absolutePath}, contents:\n$json")
        RepoManager.writeJson(json, itemFile)

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
            resultItem.openInEditor(instantSave = true, message = false)
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

    // TODO: Add a command for refreshing nbt of a specific item in the repo
    // TODO: Also use Empa's pr for tab completion of this
    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("sheditrepoitem") {
            description = "Open the RepoItemEditor for the specified internal name"
            category = CommandCategory.DEVELOPER_TEST
            argCallback("internalName", BrigadierArguments.string()) { internalName ->
                val item = internalName.toInternalName().getItemStackOrNull()
                if (item == null) {
                    ChatUtils.chat("§cCould not find item with internal name §e$internalName§c.")
                    return@argCallback
                }
                item.openInEditor(instantSave = false)
            }
        }
    }
}
