package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod.launchCoroutine
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.coroutines.CoroutineConfig
import org.lwjgl.util.tinyfd.TinyFileDialogs
import kotlin.time.Duration

object DialogUtils {

    fun openPopupWindow(title: String, message: String) =
        CoroutineConfig("openPopupWindow $title", timeout = Duration.INFINITE).launchCoroutine {
            runCatching {
                TinyFileDialogs.tinyfd_messageBox(
                    title,
                    message,
                    "ok",
                    "info",
                    true,
                )
            }.onFailure { e ->
                ErrorManager.logErrorWithData(
                    e, "Failed to open a popup window",
                    "title" to title,
                    "message" to message,
                )
            }
        }
}
