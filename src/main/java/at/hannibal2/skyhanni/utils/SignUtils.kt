package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.utils.StringUtils.capAtMinecraftLength
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.unformattedTextCompat
import at.hannibal2.skyhanni.utils.coroutines.CoroutineConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen
import net.minecraft.client.gui.screens.inventory.SignEditScreen
import net.minecraft.network.chat.Component

object SignUtils {
    private var pasteLastClicked = false
    private var copyLastClicked = false
    private var deleteLastClicked = false

    private val deleteConfig = CoroutineConfig("sign utils check deleting")
    private val copyConfig = CoroutineConfig("sign utils copy copying")
    private val pasteConfig = CoroutineConfig("sign utils paste config")

    fun setTextIntoSign(text: String, line: Int = 0) {
        val gui = Minecraft.getInstance().screen
        if (gui !is AbstractSignEditScreen) return
        val oldRow = gui.line
        gui.line = line
        gui.setMessage(text)
        gui.line = oldRow
    }

    private fun addTextIntoSign(addedText: String) {
        val gui = Minecraft.getInstance().screen
        if (gui !is AbstractSignEditScreen) return
        val lines = gui.signText
        val index = gui.line
        val text = lines[index].unformattedTextCompat() + addedText
        lines[index] = text.capAtMinecraftLength(91).asComponent()
    }

    fun checkDeleting(gui: Screen?) {
        val deleteClicked = KeyboardManager.isDeleteWordDown() || KeyboardManager.isDeleteLineDown()
        if (!deleteLastClicked && deleteClicked && gui is AbstractSignEditScreen) deleteConfig.launch {
            val newLine = if (KeyboardManager.isDeleteLineDown()) ""
            else if (KeyboardManager.isDeleteWordDown()) {
                val currentLine = gui.signText[gui.line].unformattedTextCompat()

                val lastSpaceIndex = currentLine.trimEnd().lastIndexOf(' ')
                if (lastSpaceIndex >= 0) currentLine.substring(0, lastSpaceIndex + 2) else ""
            } else return@launch
            setTextIntoSign(newLine, gui.line)
        }
        deleteLastClicked = deleteClicked
    }

    fun checkCopying(gui: Screen?) {
        val copyClicked = KeyboardManager.isCopyingKeysDown()
        if (!copyLastClicked && copyClicked && gui is AbstractSignEditScreen) copyConfig.launch {
            val newLine = gui.signText[gui.line].unformattedTextCompat()
            val copied = OSUtils.copyToClipboardAsync(newLine) ?: false
            if (!copied) ChatUtils.chat("§cFailed to copy sign text to clipboard")
        }
        copyLastClicked = copyClicked
    }

    fun checkPaste() {
        val pasteClicked = KeyboardManager.isPastingKeysDown()
        if (!pasteLastClicked && pasteClicked) pasteConfig.launch {
            OSUtils.readFromClipboard()?.let(::addTextIntoSign)
        }
        pasteLastClicked = pasteClicked
    }

    private fun SignEditScreen.getSignLines(): List<String> {
        return (this as AbstractSignEditScreen).signText.map { it.unformattedTextCompat().removeColor() }
    }

    fun SignEditScreen.isRancherSign(): Boolean {
        val signText = getSignLines().takeIfNotEmpty() ?: return false
        // one of the signs say "Set your Garden's" but because its too long (on 1.8) the word garden doesn't get rendered
        return signText[1] == "^^^^^^" && signText[2].startsWith("Set your") && signText[3].endsWith("speed cap!")
    }

    fun SignEditScreen.isMousematSign(): Boolean {
        val signText = getSignLines().takeIfNotEmpty() ?: return false
        return signText[1] == "Set Yaw Above!" && signText[2] == "Set Pitch Below!"
    }

    fun SignEditScreen.isBazaarSign(): Boolean {
        val signText = getSignLines().takeIfNotEmpty() ?: return false
        if (signText[1] == "^^^^^^^^^^^^^^^" && signText[2] == "Enter amount" && signText[3] == "to order") return true // Bazaar buy
        if (signText[1] == "^^^^^^^^^^^^^^^" && signText[2] == "Enter amount" && signText[3] == "to sell") return true // Bazaar sell
        return false
    }

    fun SignEditScreen.isSupercraftAmountSetSign(): Boolean {
        val signText = getSignLines().takeIfNotEmpty() ?: return false
        return signText[1] == "^^^^^^" && signText[2] == "Enter amount" && signText[3] == "of crafts"
    }

    fun SignEditScreen.isGardenSign(): Boolean {
        return isRancherSign() || isMousematSign()
    }

    fun SignEditScreen.isPlayerElectionSign(): Boolean {
        val signText = getSignLines().takeIfNotEmpty() ?: return false
        return signText[2] == "Cast your" && signText[3] == "vote"
    }

    private val AbstractSignEditScreen.signText: Array<Component>
        get() = this.text.getMessages(false)
}
