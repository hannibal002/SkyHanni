package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.async
import at.hannibal2.skyhanni.SkyHanniMod.launchCoroutine
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import com.mojang.blaze3d.platform.ClipboardManager
import net.minecraft.client.Minecraft

object ClipboardUtils {

    private val config get() = SkyHanniMod.feature.misc

    private val clipboardCoroutineSettings = CoroutineSettings(
        "clipboardAccess",
        withIOContext = true,
    )

    fun copyToClipboardAsyncWithResponse(text: String, step: Int = 0, info: String? = null) {
        val name = info ?: "Information"

        CoroutineSettings("copyToClipboard $name").launchCoroutine {
            val copied = copyToClipboardAsync(text, step) ?: false
            ChatUtils.chat(if (copied) "$name was copied to clipboard." else "§cFailed to copy $name to clipboard.")
        }
    }

    suspend fun copyToClipboardAsync(text: String, step: Int = 0): Boolean? = clipboardCoroutineSettings.async {
        copyToClipboardInternal(text, step)
    }.await()

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

    fun shouldCopyAutomatically() = config.copyInfoToClipboard
}
