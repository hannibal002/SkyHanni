package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import me.shedaniel.math.Rectangle
import me.shedaniel.rei.api.client.plugins.REIClientPlugin
import me.shedaniel.rei.api.client.registry.screen.DisplayBoundsProvider
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.InteractionResult
import kotlin.jvm.java

class SkyHanniReiPlugin : REIClientPlugin {
    override fun registerScreens(registry: ScreenRegistry) {
        registry.registerDecider(SkyHanniDisplayBoundsProvider())
    }
}

private class SkyHanniDisplayBoundsProvider : DisplayBoundsProvider<SkyHanniBaseScreen> {
    override fun <R : Screen> shouldScreenBeOverlaid(screen: R): InteractionResult {
        val customScreen = screen as? SkyHanniBaseScreen ?: return InteractionResult.PASS
        return if (customScreen.shouldShowItemList()) InteractionResult.CONSUME else InteractionResult.PASS
    }

    override fun getScreenBounds(screen: SkyHanniBaseScreen): Rectangle {
        val dimensions = screen.getDimensions()
        val topLeft = screen.getTopLeft()
        return Rectangle(topLeft.first, topLeft.second, dimensions.first, dimensions.second)
    }

    override fun <R : Screen> isHandingScreen(screen: Class<R>): Boolean {
        return screen.isAssignableFrom(SkyHanniBaseScreen::class.java)
    }
}
