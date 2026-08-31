package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.inventory.wardrobe.ArmorWardrobeApi
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobe
import com.operationpotato.itemlist.api.ExcludedScreensManager
import com.operationpotato.itemlist.api.ExclusionZoneManager
import com.operationpotato.itemlist.api.Plugin
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.renderer.Rect2i
import java.util.Optional

object SkyBlockItemListPlugin : Plugin {

    private val showReiItems get() = SkyHanniMod.feature.inventory.customWardrobe.showReiItems

    // Getting the hovered item in SBIL requires a key event,
    // so all the currently existing stackUnderCursor() would need to get reworked.

    override fun registerExcludedScreens(excludedScreensManager: ExcludedScreensManager) {
        excludedScreensManager.addProvider(ContainerScreen::class.java) { screen ->
            if (ArmorWardrobeApi.inCustomWardrobe && (CustomWardrobe.renderableTopCorner == Pair(0, 0) || !showReiItems)) {
                return@addProvider Optional.of("SkyHanni Wardrobe")
            }
            Optional.empty()
        }
    }

    override fun registerExclusionZones(exclusionZoneManager: ExclusionZoneManager) {
        exclusionZoneManager.addProvider(Screen::class.java) { screen ->
            if (ArmorWardrobeApi.inCustomWardrobe && (CustomWardrobe.renderableTopCorner != Pair(0, 0) && showReiItems)) {
                return@addProvider listOf(
                    Rect2i(
                        CustomWardrobe.renderableTopCorner.first, CustomWardrobe.renderableTopCorner.second,
                        CustomWardrobe.renderableDimensions.first, CustomWardrobe.renderableDimensions.second,
                    ),
                )
            }
            listOf()
        }
    }

}
