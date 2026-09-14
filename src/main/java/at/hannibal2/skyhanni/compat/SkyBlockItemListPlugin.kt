package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import com.operationpotato.itemlist.api.Plugin
import com.operationpotato.itemlist.api.supportedscreen.ScreenBounds
import com.operationpotato.itemlist.api.supportedscreen.SupportedScreenManager
import com.operationpotato.itemlist.api.HoveredItemManager
import java.util.Optional

object SkyBlockItemListPlugin : Plugin {

    override fun registerSupportedScreens(manager: SupportedScreenManager) {
        manager.addProvider(SkyHanniBaseScreen::class.java) { screen, _, _ ->
            if (screen.shouldShowItemList()) {
                return@addProvider Optional.of(ScreenBounds(screen.rectangle.left(), screen.rectangle.right()))
            }
            return@addProvider Optional.empty()
        }
    }

    override fun registerHoveredItems(manager: HoveredItemManager) {
        manager.addConsumer { screen, stack, _ ->
            if (screen !is SkyHanniGuiContainer) return@addConsumer false
            val event = GuiKeyPressEvent.GuiKeyboardKeyPressEvent(screen, stack)
            event.post()
            return@addConsumer event.isCancelled
        }
    }
}
