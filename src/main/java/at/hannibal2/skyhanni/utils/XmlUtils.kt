package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import io.github.notenoughupdates.moulconfig.xml.XMLUniverse
import net.minecraft.network.chat.Component

object XmlUtils {

    fun openXmlScreen(bind: Any, resource: MyResourceLocation) {
        val universe = XMLUniverse.getDefaultUniverse()
        val context = GuiContext(universe.load(bind, resource))
        SkyHanniMod.screenToOpen = MoulConfigScreenComponent(Component.empty(), context, null)
    }

}
