package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiEditSign
import at.hannibal2.skyhanni.utils.StringUtils.capAtMinecraftLength
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.util.IChatComponent

object SignUtils {
    private var pasteLastClicked = false
    private var copyLastClicked = false
    private var deleteLastClicked = false

    fun setTextIntoSign(text: String, line: Int = 0) {
        val gui = Minecraft.getMinecraft().currentScreen
        if (gui !is AccessorGuiEditSign) return
        gui.text[line] = text.asComponent()
    }

    private fun addTextIntoSign(addedText: String) {
        val gui = Minecraft.getMinecraft().currentScreen
        if (gui !is AccessorGuiEditSign) return
        val lines = gui.text
        val index = gui.editLine
        val text = lines[index].unformattedText + addedText
        lines[index] = text.capAtMinecraftLength(91).asComponent()
    }

    fun checkDeleting(gui: GuiScreen?) {
        val deleteClicked = KeyboardManager.isDeleteWordDown() || KeyboardManager.isDeleteLineDown()
        if (!deleteLastClicked && deleteClicked && gui is AccessorGuiEditSign) {
            SkyHanniMod.coroutineScope.launch {
                val newLine = if (KeyboardManager.isDeleteLineDown()) ""
                else if (KeyboardManager.isDeleteWordDown()) {
                    val currentLine = gui.text[gui.editLine].unformattedText

                    val lastSpaceIndex = currentLine.trimEnd().lastIndexOf(' ')
                    if (lastSpaceIndex >= 0) currentLine.substring(0, lastSpaceIndex + 2) else ""
                } else return@launch
                setTextIntoSign(newLine, gui.editLine)
            }
        }
        deleteLastClicked = deleteClicked
    }

    fun checkCopying(gui: GuiScreen?) {
        val copyClicked = KeyboardManager.isCopyingKeysDown()
        if (!copyLastClicked && copyClicked && gui is AccessorGuiEditSign) {
            SkyHanniMod.coroutineScope.launch {
                ClipboardUtils.copyToClipboard(gui.text[gui.editLine].unformattedText)
            }
        }
        copyLastClicked = copyClicked
    }

    fun checkPaste() {
        val pasteClicked = KeyboardManager.isPastingKeysDown()
        if (!pasteLastClicked && pasteClicked) {
            SkyHanniMod.coroutineScope.launch {
                OSUtils.readFromClipboard()?.let {
                    addTextIntoSign(it)
                }
            }
        }
        pasteLastClicked = pasteClicked
    }

    private fun GuiEditSign.getSignLines(): List<String>? {
        if (this !is AccessorGuiEditSign) return null
        return (this as AccessorGuiEditSign).text.map { it.unformattedText.removeColor() }
    }

    fun GuiEditSign.isRancherSign(): Boolean {
        val signText = getSignLines() ?: return false
        return signText[1] == "^^^^^^" && signText[2] == "Set your" && signText[3] == "speed cap!"
    }

    fun GuiEditSign.isMousematSign(): Boolean {
        val signText = getSignLines() ?: return false
        return signText[1] == "Set Yaw Above!" && signText[2] == "Set Pitch Below!"
    }

    fun GuiEditSign.isBazaarSign(): Boolean {
        val signText = getSignLines() ?: return false
        if (signText[1] == "^^^^^^^^^^^^^^^" && signText[2] == "Enter amount" && signText[3] == "to order") return true // Bazaar buy
        if (signText[1] == "^^^^^^^^^^^^^^^" && signText[2] == "Enter amount" && signText[3] == "to sell") return true // Bazaar sell
        return false
    }

    fun GuiEditSign.isSupercraftAmountSetSign(): Boolean {
        val signText = getSignLines() ?: return false
        return signText[1] == "^^^^^^" && signText[2] == "Enter amount" && signText[3] == "of crafts"
    }

    fun GuiEditSign.isGardenSign(): Boolean {
        return isRancherSign() || isMousematSign()
    }

    val AccessorGuiEditSign.text: Array<IChatComponent>
        //#if MC < 1.21
        get() = this.tileSign.signText
    //#else
    //$$ get() = this.tileSign.frontText.getMessages(false)
    //#endif
}
