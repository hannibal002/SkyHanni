package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import com.operationpotato.itemlist.api.Plugin
import com.operationpotato.itemlist.api.supportedscreen.ScreenBounds
import com.operationpotato.itemlist.api.supportedscreen.SupportedScreenManager
import java.util.Optional

object SkyBlockItemListPlugin : Plugin {

    // Getting the hovered item in SBIL requires a key event,
    // so all the currently existing stackUnderCursor() would need to get reworked.

    override fun registerSupportedScreens(manager: SupportedScreenManager) {
        manager.addProvider(SkyHanniBaseScreen::class.java) { screen, _, _ ->
            if (screen.shouldShowItemList()) {
                return@addProvider Optional.of(ScreenBounds(screen.rectangle.left(), screen.rectangle.right()))
            }
            return@addProvider Optional.empty()
        }
    }

}
