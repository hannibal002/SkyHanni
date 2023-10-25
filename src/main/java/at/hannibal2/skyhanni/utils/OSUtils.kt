package at.hannibal2.skyhanni.utils

import java.awt.Desktop
import java.io.IOException
import java.net.URI

object OSUtils {

    @JvmStatic
    fun openBrowser(url: String) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(URI(url))
            } catch (e: IOException) {
                e.printStackTrace()
                LorenzUtils.error("[SkyHanni] Error opening website: $url!")
            }
        } else {
            copyToClipboard(url)
            LorenzUtils.error("[SkyHanni] Web browser is not supported! Copied url to clipboard.")
        }
    }

    fun copyToClipboard(text: String) {
        ClipboardUtils.copyToClipboard(text)
    }

    suspend fun readFromClipboard() = ClipboardUtils.readFromClipboard()
}
