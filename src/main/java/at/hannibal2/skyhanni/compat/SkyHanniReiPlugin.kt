package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.inventory.wardrobe.AbstractCustomMenuScreen
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobeScreen
import me.shedaniel.math.Rectangle
import me.shedaniel.rei.api.client.registry.screen.DisplayBoundsProvider
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.InteractionResult

class SkyHanniReiPlugin : DisplayBoundsProvider<AbstractCustomMenuScreen> {
    private fun Screen.fullRectangle(): Rectangle {
        return Rectangle(0, 0, this.width, this.height)
    }

    private fun Screen.customExclusionRect(): Rectangle {
        return when(this) {
            is CustomWardrobeScreen -> {
                Rectangle(
                    this.renderableTopCorner.first, this.renderableTopCorner.second,
                    this.renderableDimensions.first, this.renderableDimensions.second,
                )
            }
            else -> this.fullRectangle()
        }
    }

    override fun <R : Screen?> shouldScreenBeOverlaid(screen: R): InteractionResult {
        val showReiItems = when(screen) {
            is CustomWardrobeScreen -> SkyHanniMod.feature.inventory.customWardrobe.showReiItems
            else -> false
        }
        return if (showReiItems) InteractionResult.SUCCESS else InteractionResult.PASS
    }

    override fun getScreenBounds(screen: AbstractCustomMenuScreen): Rectangle {
        return screen.customExclusionRect()
    }

    override fun <R : Screen> isHandingScreen(screen: Class<R>): Boolean {
        return screen == CustomWardrobeScreen::class.java
    }
}
