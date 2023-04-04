package at.hannibal2.skyhanni.utils

import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.net.URI

object OSUtils {

    fun openBrowser(url: String) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(URI(url))
            } catch (e: IOException) {
                e.printStackTrace()
                LorenzUtils.error("[SkyHanni] Error opening website!")
            }
        } else {
            LorenzUtils.warning("[SkyHanni] Web browser is not supported!")
        }
    }

    fun copyToClipboard(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

    fun readFromClipboard(): String? {
        val systemClipboard = Toolkit.getDefaultToolkit().systemClipboard ?: return null
        try {
            val data = systemClipboard.getData(DataFlavor.stringFlavor) ?: return null
            return data.toString()
        } catch (e: UnsupportedFlavorException) {
            return null
        }
    }

    fun KeyBinding.isActive() : Boolean {
        if (!Keyboard.isCreated()) return false
        try {
            if (Keyboard.isKeyDown(this.keyCode)) return true
        } catch (e: IndexOutOfBoundsException) {
            println("KeyBinding isActive caused an IndexOutOfBoundsException with keyCode: $keyCode")
            e.printStackTrace()
            return false
        }
        if (this.isKeyDown || this.isPressed) return true
        return false
    }
}