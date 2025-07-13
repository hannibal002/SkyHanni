package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.system.ModVersion
import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class EnforcedConfigValuesJson(
    @Expose @SerializedName("enforced_config_values") val enforcedConfigValues: List<EnforcedValueData>,
)

data class EnforcedValueData(
    @Expose var enforcedValues: List<EnforcedValue> = listOf(),
    @Expose var notificationPSA: List<String>? = null,
    @Expose var chatPSA: List<String>? = null,
    @Expose var affectedVersion: ModVersion,
    @Expose var affectedMinecraftVersions: List<String>? = null,
    @Expose var extraMessage: String? = null,
)

data class EnforcedValue(
    @Expose var path: String,
    @Expose var value: JsonElement,
)
