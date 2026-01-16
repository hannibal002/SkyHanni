package at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.storage.SpecificSeaCreatureStorage
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.observer.ObservableList
import io.github.notenoughupdates.moulconfig.xml.Bind


class SpecificSeaCreatureStorageXMLHelper(
    private val from: SpecificSeaCreatureSettings,
    private val seaCreatureList: ObservableList<SpecificSeaCreatureStorageXMLHelper>
) {
    @field:Bind
    var name: String = from.name

    @field:Bind
    var shouldRenderLootshare: Boolean = from.shouldRenderLootshare

    @field:Bind
    var shouldShowHealthOverlay: Boolean = from.shouldShowHealthOverlay

    @field:Bind
    var shouldShareInChat: Boolean = from.shouldShareInChat

    @field:Bind
    var shouldShowKillTime: Boolean = from.shouldShowKillTime


    @Bind
    fun getName(): StructuredText {
        return name.asStructuredText()
    }

    @Bind
    fun afterClose() {
        SpecificSeaCreatures.save(seaCreatureList)
    }
}

