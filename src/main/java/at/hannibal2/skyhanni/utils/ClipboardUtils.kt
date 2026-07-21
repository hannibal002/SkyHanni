package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod.async
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import com.mojang.blaze3d.platform.ClipboardManager
import kotlinx.coroutines.Deferred
import net.minecraft.client.Minecraft

object ClipboardUtils {

    private val clipboardCoroutineSettings = CoroutineSettings(
        "clipboardAccess",
        withIOContext = true,
    )

    @Deprecated("Use copyToClipboardAsync instead", ReplaceWith("copyToClipboardAsync(text).await()"))
    fun copyToClipboard(text: String, step: Int = 0) = copyToClipboardInternal(text, step)

    fun copyToClipboardAsync(text: String, step: Int = 0): Deferred<Boolean?> = clipboardCoroutineSettings.async {
        copyToClipboardInternal(text, step)
    }

    private fun copyToClipboardInternal(text: String, step: Int = 0): Boolean = runCatching {
        ClipboardManager().setClipboard(Minecraft.getInstance().window, text)
        true
    }.getOrElse {
        if (step == 3) {
            ErrorManager.logErrorWithData(it, "Error while trying to access the clipboard.")
            false
        } else copyToClipboardInternal(text, step + 1)
    }

    fun readFromClipboard(step: Int = 0): String? {
        var shouldRetry = false
        val clipboard = ClipboardManager().getClipboard(Minecraft.getInstance().window) { _, _ ->
            shouldRetry = true
        }
        return if (!shouldRetry) clipboard
        else if (step == 3) {
            ErrorManager.logErrorStateWithData(
                "can not read clipboard",
                "clipboard can not be accessed after 3 retries",
            )
            null
        } else readFromClipboard(step + 1)
    }
}
