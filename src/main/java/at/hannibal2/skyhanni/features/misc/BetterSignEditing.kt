package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiEditSign
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

class BetterSignEditing {
    private var pasteLastClicked = false
    private var copyLastClicked = false
    private var deleteWordLastClicked = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.onHypixel) return
        if (!SkyHanniMod.feature.misc.betterSignEditing) return

        val gui = Minecraft.getMinecraft().currentScreen
        checkPaste()
        checkCopying(gui)
        checkDeleting(gui)
    }

    private fun checkDeleting(gui: GuiScreen?) {
        val deleteWordClicked = Keyboard.KEY_BACK.isKeyHeld() && KeyboardManager.isModifierKeyDown()
        if (!deleteWordLastClicked && deleteWordClicked && gui is AccessorGuiEditSign) {
            SkyHanniMod.coroutineScope.launch {
                val newLine = if (KeyboardManager.isShiftKeyDown()) "" else {
                    val currentLine = gui.tileSign.signText[gui.editLine].unformattedText
                    val lastSpaceIndex = currentLine.lastIndexOf(' ')
                    if (lastSpaceIndex >= 0) currentLine.substring(0, lastSpaceIndex + 1) else ""
                }
                LorenzUtils.setTextIntoSign(newLine, gui.editLine)
            }
        }
        deleteWordLastClicked = deleteWordClicked
    }

    private fun checkCopying(gui: GuiScreen?) {
        val copyClicked = KeyboardManager.isCopyingKeysDown()
        if (!copyLastClicked && copyClicked && gui is AccessorGuiEditSign) {
            SkyHanniMod.coroutineScope.launch {
                ClipboardUtils.copyToClipboard(gui.tileSign.signText[gui.editLine].unformattedText)
            }
        }
        copyLastClicked = copyClicked
    }

    private fun checkPaste() {
        val pasteClicked = KeyboardManager.isPastingKeysDown()
        if (!pasteLastClicked && pasteClicked) {
            SkyHanniMod.coroutineScope.launch {
                OSUtils.readFromClipboard()?.let {
                    LorenzUtils.addTextIntoSign(it)
                }
            }
        }
        pasteLastClicked = pasteClicked
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(16, "misc.pasteIntoSigns", "misc.betterSignEditing")
    }
}
