package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobe
import me.shedaniel.math.Rectangle
import me.shedaniel.rei.api.client.plugins.REIClientPlugin
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones
import net.minecraft.client.gui.screens.inventory.ContainerScreen

class SkyHanniReiPlugin : REIClientPlugin {

    override fun registerExclusionZones(zones: ExclusionZones) {
        zones.register(ContainerScreen::class.java) { screen ->
            if (CustomWardrobe.isOverlayVisible) {
                if (CustomWardrobe.renderableTopCorner == Pair(0, 0)) {
                    listOf(screen.fullRectangle())
                } else {
                    listOf(screen.customWardrobeExclusionRect())
                }
            } else {
                listOf()
            }
        }
    }

    private fun ContainerScreen.fullRectangle(): Rectangle {
        return Rectangle(0, 0, this.width, this.height)
    }

    private fun ContainerScreen.customWardrobeExclusionRect(): Rectangle {
        if (!SkyHanniMod.feature.inventory.customWardrobe.showReiItems.get()) {
            return fullRectangle()
        }
        return Rectangle(
            CustomWardrobe.renderableTopCorner.first, CustomWardrobe.renderableTopCorner.second,
            CustomWardrobe.renderableDimensions.first, CustomWardrobe.renderableDimensions.second,
        )
    }
}
