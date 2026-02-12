package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager
import org.lwjgl.util.tinyfd.TinyFileDialogs

object DialogUtils {

    fun openPopupWindow(title: String, message: String) = SkyHanniMod.launchCoroutine("openPopupWindow $title") {
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
