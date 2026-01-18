package at.hannibal2.skyhanni.features.event.diana.dianararemobstorage

import at.hannibal2.skyhanni.utils.KSerializable
import com.google.gson.annotations.Expose

@KSerializable
data class RareDianaMobSettings(
    @Expose var name: String,
    @Expose var shouldShareOnDiscovery: Boolean?,
)
