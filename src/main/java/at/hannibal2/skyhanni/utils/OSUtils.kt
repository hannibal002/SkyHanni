package at.hannibal2.skyhanni.utils

import java.awt.Desktop
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
}