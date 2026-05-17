package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.utils.system.MCVersion
import at.hannibal2.skyhanni.utils.system.ModVersion
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ModrinthRelease(
    @Expose @SerializedName("game_versions") val gameVersions: List<MCVersion>,
    @Expose val id: String,
    @Expose @SerializedName("version_number") val versionNumber: ModVersion,
    @Expose val changelog: String?,
    @Expose @SerializedName("version_type") val versionType: ModrinthVersionType,
)
