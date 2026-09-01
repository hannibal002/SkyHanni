package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.system.ModVersion
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class DisabledEventsJson(
    @Expose @SerializedName(value = "disabled_handlers", alternate = ["disabledHandlers"]) val disabledHandlers: Set<String> = emptySet(),
    @Expose @SerializedName(value = "disabled_invokers", alternate = ["disabledInvokers"]) val disabledInvokers: Set<String> = emptySet(),
    @Expose @SerializedName("disabled_handlers_versioned")
    val disabledHandlersVersioned: Set<DisabledEventVersionedJson> = emptySet(),
    @Expose @SerializedName("disabled_invokers_versioned")
    val disabledInvokersVersioned: Set<DisabledEventVersionedJson> = emptySet(),
)

data class DisabledEventVersionedJson(
    @Expose val name: String,
    @Expose @SerializedName("min_version") val minVersion: ModVersion? = null,
    @Expose @SerializedName("max_version") val maxVersion: ModVersion? = null,
    @Expose @SerializedName("mc_versions") val mcVersions: Set<String>? = null,
)
