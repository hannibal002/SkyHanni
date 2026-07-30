package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.inventory.wardrobe.AbstractCustomMenuScreen
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobeScreen
import me.shedaniel.math.Rectangle
import me.shedaniel.rei.api.client.plugins.REIClientPlugin
import me.shedaniel.rei.api.client.registry.screen.DisplayBoundsProvider
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.InteractionResult

class SkyHanniReiPlugin : REIClientPlugin {
    override fun registerScreens(registry: ScreenRegistry) {
        registry.registerDecider(CustomDisplayBoundsProvider())
    }
}

private class CustomDisplayBoundsProvider :
    DisplayBoundsProvider<AbstractCustomMenuScreen> {

    private fun Screen.fullRectangle() =
        Rectangle(0, 0, width, height)

    private fun Screen.customExclusionRect() =
        when (this) {
            is CustomWardrobeScreen -> Rectangle(
                renderableTopCorner.first,
                renderableTopCorner.second,
                renderableDimensions.first,
                renderableDimensions.second,
            )

            else -> fullRectangle()
        }

    override fun <R : Screen?> shouldScreenBeOverlaid(screen: R): InteractionResult =
        when (screen) {
            is CustomWardrobeScreen ->
                if (SkyHanniMod.feature.inventory.customWardrobe.showReiItems) {
                    InteractionResult.SUCCESS
                } else {
                    InteractionResult.PASS
                }

            else -> InteractionResult.PASS
        }

    override fun getScreenBounds(screen: AbstractCustomMenuScreen): Rectangle =
        screen.customExclusionRect()

    override fun <R : Screen> isHandingScreen(screen: Class<R>): Boolean =
        screen == CustomWardrobeScreen::class.java
}
