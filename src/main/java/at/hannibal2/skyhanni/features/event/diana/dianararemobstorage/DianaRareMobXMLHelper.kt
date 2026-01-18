package at.hannibal2.skyhanni.features.event.diana.dianararemobstorage

import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.xml.Bind

class DianaRareMobXMLHelper(
    private val from: RareDianaMobSettings,
) {
    @field:Bind
    var name: String = from.name

    @field:Bind
    var shouldShareOnDiscovery: Boolean? = from.shouldShareOnDiscovery

    @Bind
    fun getName(): StructuredText {
        return name.asStructuredText()
    }
}
