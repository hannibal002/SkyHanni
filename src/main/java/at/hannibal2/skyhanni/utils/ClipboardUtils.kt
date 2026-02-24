package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.client.Minecraft

object ClipboardUtils {

    fun copyToClipboard(text: String, step: Int = 0) {
        SkyHanniMod.launchCoroutine("copyToClipboard") {
            try {
                com.mojang.blaze3d.platform.ClipboardManager().setClipboard(Minecraft.getInstance().window, text)
            } catch (e: Exception) {
                if (step == 3) {
                    ErrorManager.logErrorWithData(e, "Error while trying to access the clipboard.")
                } else {
                    copyToClipboard(text, step + 1)
                }
            }
        }
    }

    fun readFromClipboard(step: Int = 0): String? {
        var shouldRetry = false
        val clipboard = com.mojang.blaze3d.platform.ClipboardManager().getClipboard(
            Minecraft.getInstance().window,
        ) { _, _ ->
            shouldRetry = true
        }
        if (shouldRetry) {
            if (step == 3) {
                ErrorManager.logErrorStateWithData(
                    "can not read clipboard",
                    "clipboard can not be accessed after 3 retries",
                )
                return null
            } else {
                return readFromClipboard(step + 1)
            }
        }
        return clipboard
    }
}
