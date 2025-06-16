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
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.slotUnderCursor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonObject
import net.minecraft.item.ItemStack
import java.io.File

@SkyHanniModule
object RepoItemEditor {

    private val patternGroup = RepoPattern.group("dev.repoitemeditor")

    private val craftingTablePattern by patternGroup.pattern(
        "craftingtable",
        "§aCrafting Table",
    )

    val config get(): RepoItemEditorConfig = SkyHanniMod.feature.dev.devTool.repoItemEditor

    @HandleEvent
    fun onKeybind(event: GuiKeyPressEvent) {
        if (!config.editModeEnabled) return
        when {
            config.openRepoItemEditorKeybind.isKeyHeld() -> attemptToOpenInEditor(instantSave = false)
            config.instantEditKeybind.isKeyHeld() -> attemptToOpenInEditor(instantSave = true)
            config.saveRecipeKeybind.isKeyHeld() -> saveRecipe()
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
                if (slotStack.isNotEmpty()) {
                    recipeJson.addProperty(recipeTileName, "${slotStack.getInternalNameOrNull()?.asString()}:${slotStack.stackSize}")
                } else {
                    recipeJson.addProperty(recipeTileName, "")
                }
            }
        }
        existingJson.add("recipe", recipeJson)
        existingJson.addProperty("clickcommand", "viewrecipe")
        existingJson.addProperty("modver", SkyHanniMod.VERSION)

        saveItemToRepo(resultInternalName, existingJson)
        ChatUtils.chat("§aSuccessfully saved recipe for ${resultInternalName.asString()} to repo folder!")
    }

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
