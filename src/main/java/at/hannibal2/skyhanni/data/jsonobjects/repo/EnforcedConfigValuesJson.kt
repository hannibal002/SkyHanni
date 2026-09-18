package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.system.ModVersion
import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class EnforcedConfigValuesJson(
    @Expose @SerializedName("enforced_config_values") val enforcedConfigValues: List<EnforcedValueData>,
)

data class EnforcedValueData(
    @Expose @SerializedName(value = "enforced_values", alternate = ["enforcedValues"]) val enforcedValues: List<EnforcedValue> = listOf(),
    @Expose @SerializedName(value = "notification_psa", alternate = ["notificationPSA"]) val notificationPSA: List<String>? = null,
    @Expose @SerializedName(value = "chat_psa", alternate = ["chatPSA"]) val chatPSA: List<String>? = null,
    @Expose
    @SerializedName(value = "minimum_affected_version", alternate = ["minimumAffectedVersion"])
    val minimumAffectedVersion: ModVersion? = null,
    @Expose @SerializedName(value = "affected_version", alternate = ["affectedVersion"]) val affectedVersion: ModVersion,
    @Expose
    @SerializedName(value = "affected_minecraft_versions", alternate = ["affectedMinecraftVersions"])
    val affectedMinecraftVersions: List<String>? = null,
    @Expose @SerializedName(value = "extra_message", alternate = ["extraMessage"]) val extraMessage: String? = null,
)

data class EnforcedValue(
    @Expose val path: String,
    @Expose val value: JsonElement,
    // Whether the enforced value also gets written to the config file instead of only being applied in memory
    @Expose val persist: Boolean = false,
)
