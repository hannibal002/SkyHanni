package at.hannibal2.skyhanni.test.repoitem

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.features.dev.RepoItemEditorConfig
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.compat.slotUnderCursor
import net.minecraft.item.ItemStack

@SkyHanniModule
object RepoItemEditor {

    val config get(): RepoItemEditorConfig = SkyHanniMod.feature.dev.devTool.repoItemEditor

    @HandleEvent(GuiKeyPressEvent::class)
    fun onKeybind() {
        if (!config.editModeEnabled) return
        val focussedSlot = slotUnderCursor() ?: return
        val stack = focussedSlot.stack?.copy() ?: return
        when {
            config.openRepoItemEditorKeybind.isKeyHeld() -> stack.openInEditor(instantSave = false)
            config.instantEditKeybind.isKeyHeld() -> stack.openInEditor(instantSave = true)
            else -> return
        }
    }

    private fun ItemStack.openInEditor(instantSave: Boolean) {
        val internalName = getInternalNameOrNull() ?: ErrorManager.skyHanniError("Cannot open item editor for item with unknown item name")
        val screen = RepoItemEditorGui(internalName, this)
        if (instantSave) {
            screen.adjustLore()
            screen.saveItem()
        } else {
            SkyHanniMod.screenToOpen = screen
        }
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
