package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.xml.XMLUniverse
//#if FORGE
import io.github.notenoughupdates.moulconfig.gui.GuiComponentWrapper
//#else
//$$ import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
//$$ import net.minecraft.text.Text
//#endif

object XmlUtils {

    fun openXmlScreen(bind: Any, resource: MyResourceLocation) {
        val universe = XMLUniverse.getDefaultUniverse()
        val context = GuiContext(universe.load(bind, resource))
        //#if FORGE
        SkyHanniMod.screenToOpen = GuiComponentWrapper(context)
        //#else
        //$$ SkyHanniMod.screenToOpen = MoulConfigScreenComponent(Text.empty(), context, null)
        //#endif
    }

}
