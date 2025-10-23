package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.system.ModVersion
import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class EnforcedConfigValuesJson(
    @Expose @SerializedName("enforced_config_values") val enforcedConfigValues: List<EnforcedValueData>,
)

data class EnforcedValueData(
    @Expose val enforcedValues: List<EnforcedValue> = listOf(),
    @Expose val notificationPSA: List<String>? = null,
    @Expose val chatPSA: List<String>? = null,
    @Expose val minimumAffectedVersion: ModVersion? = null,
    @Expose val affectedVersion: ModVersion,
    @Expose val affectedMinecraftVersions: List<String>? = null,
    @Expose val extraMessage: String? = null,
)

data class EnforcedValue(
    @Expose val path: String,
    @Expose val value: JsonElement,
)
