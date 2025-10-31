package at.hannibal2.hanni.compat

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.features.inventory.wardrobe.CustomWardrobe
import at.hannibal2.hanni.features.inventory.wardrobe.WardrobeApi
import me.shedaniel.math.Rectangle
import me.shedaniel.rei.api.client.plugins.REIClientPlugin
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen

class HanniReiPlugin : REIClientPlugin {

    override fun registerExclusionZones(zones: ExclusionZones) {
        zones.register(GenericContainerScreen::class.java) { screen ->
            if (WardrobeApi.inCustomWardrobe) {
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

    private fun GenericContainerScreen.fullRectangle(): Rectangle {
        return Rectangle(0, 0, this.width, this.height)
    }

    private fun GenericContainerScreen.customWardrobeExclusionRect(): Rectangle {
        val showReiItems = HanniMod.feature.inventory.customWardrobe.showReiItems
        if (!showReiItems) {
            return fullRectangle()
        }
        return Rectangle(
            CustomWardrobe.renderableTopCorner.first, CustomWardrobe.renderableTopCorner.second,
            CustomWardrobe.renderableDimensions.first, CustomWardrobe.renderableDimensions.second,
        )
    }
}
