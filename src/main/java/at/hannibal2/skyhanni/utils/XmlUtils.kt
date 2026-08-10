package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ConfigUtils.asMoulLocation
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import io.github.notenoughupdates.moulconfig.xml.XMLUniverse
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

object XmlUtils {
    fun openXmlScreen(bind: Any, resource: Identifier) {
        val universe = XMLUniverse.getDefaultUniverse()
        val context = GuiContext(universe.load(bind, resource.asMoulLocation()))
        SkyHanniMod.screenToOpen = MoulConfigScreenComponent(Component.empty(), context, null)
    }
}
