package at.hannibal2.skyhanni.utils import at.hannibal2.skyhanni.utils.compat.unformattedTextCompat

import at.hannibal2.skyhanni.SkyHanniMod
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen
import at.hannibal2.skyhanni.utils.StringUtils.capAtMinecraftLength
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.SignEditScreen
import net.minecraft.text.Text

object SignUtils {
    private var pasteLastClicked = false
    private var copyLastClicked = false
    private var deleteLastClicked = false

    fun setTextIntoSign(text: String, line: Int = 0) {
        val gui = MinecraftClient.getInstance().currentScreen
        if (gui !is AbstractSignEditScreen) return
        //#if MC < 1.21
        //$$ gui.signText[line] = text.asComponent()
        //#else
        val oldRow = gui.currentRow
        gui.currentRow = line
        gui.setCurrentRowMessage(text)
        gui.currentRow = oldRow
        //#endif
    }

    private fun addTextIntoSign(addedText: String) {
        val gui = MinecraftClient.getInstance().currentScreen
        if (gui !is AbstractSignEditScreen) return
        val lines = gui.signText
        val index = gui.currentRow
        val text = lines[index].unformattedTextCompat() + addedText
        lines[index] = text.capAtMinecraftLength(91).asComponent()
    }

    fun checkDeleting(gui: Screen?) {
        val deleteClicked = KeyboardManager.isDeleteWordDown() || KeyboardManager.isDeleteLineDown()
        if (!deleteLastClicked && deleteClicked && gui is AbstractSignEditScreen) {
            SkyHanniMod.launchCoroutine {
                val newLine = if (KeyboardManager.isDeleteLineDown()) ""
                else if (KeyboardManager.isDeleteWordDown()) {
                    val currentLine = gui.signText[gui.currentRow].unformattedTextCompat()

                    val lastSpaceIndex = currentLine.trimEnd().lastIndexOf(' ')
                    if (lastSpaceIndex >= 0) currentLine.substring(0, lastSpaceIndex + 2) else ""
                } else return@launchCoroutine
                setTextIntoSign(newLine, gui.currentRow)
            }
        }
        deleteLastClicked = deleteClicked
    }

    fun checkCopying(gui: Screen?) {
        val copyClicked = KeyboardManager.isCopyingKeysDown()
        if (!copyLastClicked && copyClicked && gui is AbstractSignEditScreen) {
            SkyHanniMod.launchCoroutine {
                ClipboardUtils.copyToClipboard(gui.signText[gui.currentRow].unformattedTextCompat())
            }
        }
        copyLastClicked = copyClicked
    }

    fun checkPaste() {
        val pasteClicked = KeyboardManager.isPastingKeysDown()
        if (!pasteLastClicked && pasteClicked) {
            SkyHanniMod.launchCoroutine {
                OSUtils.readFromClipboard()?.let {
                    addTextIntoSign(it)
                }
            }
        }
        pasteLastClicked = pasteClicked
    }

    private fun SignEditScreen.getSignLines(): List<String>? {
        if (this !is AbstractSignEditScreen) return null
        return (this as AbstractSignEditScreen).signText.map { it.unformattedTextCompat().removeColor() }
    }

    fun SignEditScreen.isRancherSign(): Boolean {
        val signText = getSignLines() ?: return false
        return signText[1] == "^^^^^^" && signText[2] == "Set your" && signText[3] == "speed cap!"
    }

    fun SignEditScreen.isMousematSign(): Boolean {
        val signText = getSignLines() ?: return false
        return signText[1] == "Set Yaw Above!" && signText[2] == "Set Pitch Below!"
    }

    fun SignEditScreen.isBazaarSign(): Boolean {
        val signText = getSignLines() ?: return false
        if (signText[1] == "^^^^^^^^^^^^^^^" && signText[2] == "Enter amount" && signText[3] == "to order") return true // Bazaar buy
        if (signText[1] == "^^^^^^^^^^^^^^^" && signText[2] == "Enter amount" && signText[3] == "to sell") return true // Bazaar sell
        return false
    }

    fun SignEditScreen.isSupercraftAmountSetSign(): Boolean {
        val signText = getSignLines() ?: return false
        return signText[1] == "^^^^^^" && signText[2] == "Enter amount" && signText[3] == "of crafts"
    }

    fun SignEditScreen.isGardenSign(): Boolean {
        return isRancherSign() || isMousematSign()
    }

    private val AbstractSignEditScreen.signText: Array<Text>
        //#if MC < 1.21
        //$$ get() = this.tileSign.signText
    //#else
    get() = this.text.getMessages(false)
    //#endif
}
