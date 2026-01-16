package at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui

import at.hannibal2.skyhanni.utils.KSerializable
import com.google.gson.annotations.Expose

@KSerializable
data class SpecificSeaCreatureSettings(
    @Expose var name: String,
    @Expose var shouldRenderLootshare: Boolean?,
    @Expose var shouldShowHealthOverlay: Boolean?,
    @Expose var shouldShareInChat: Boolean?,
    @Expose var shouldShowKillTime: Boolean?,
)
