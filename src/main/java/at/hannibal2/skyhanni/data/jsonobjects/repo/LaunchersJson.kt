package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class LaunchersJson(
    @Expose val launchers: List<LauncherEntry>,
    @Expose @SerializedName(value = "generic_stacks", alternate = ["genericStacks"]) val genericStacks: List<String>,
)

data class LauncherEntry(
    @Expose val name: String,
    @Expose @SerializedName(value = "first_stacks", alternate = ["firstStacks"]) val firstStacks: List<String>,
    @Expose val brand: String? = null,
    @Expose val flagged: Boolean = false,
) {
    fun getIdPair() = name to flagged
}
