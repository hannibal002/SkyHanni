package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import me.shedaniel.math.Rectangle
import me.shedaniel.rei.api.client.plugins.REIClientPlugin
import me.shedaniel.rei.api.client.registry.screen.DisplayBoundsProvider
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.InteractionResult

class SkyHanniReiPlugin : REIClientPlugin {
    override fun registerScreens(registry: ScreenRegistry) {
        registry.registerDecider(SkyHanniDisplayBoundsProvider())
    }
}

private class SkyHanniDisplayBoundsProvider : DisplayBoundsProvider<SkyHanniBaseScreen> {
    override fun <R : Screen?> shouldScreenBeOverlaid(screen: R?): InteractionResult? {
        val customScreen = screen as? SkyHanniBaseScreen ?: return InteractionResult.PASS
        return if (customScreen.shouldShowItemList()) {
            InteractionResult.SUCCESS
        } else {
            InteractionResult.FAIL
        }
    }

    override fun getScreenBounds(screen: SkyHanniBaseScreen): Rectangle? {
        // If the screen does not want to show the item list, return null to indicate not to show the overlay
        // Should not happen because shouldScreenBeOverlaid should have been called first, but just in case
        if (!screen.shouldShowItemList()) return null
        val rectangle = screen.rectangle
        val position = rectangle.position
        return Rectangle(position.x, position.y, rectangle.width, rectangle.height)
    }

    override fun <R : Screen> isHandingScreen(screen: Class<R>): Boolean {
        return SkyHanniBaseScreen::class.java.isAssignableFrom(screen)
    }
}
