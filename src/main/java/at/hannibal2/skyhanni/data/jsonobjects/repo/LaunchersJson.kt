package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class LaunchersJson(
    @Expose val launchers: List<LauncherEntry>,
    @Expose val genericStacks: List<String>,
)

data class LauncherEntry(
    @Expose val name: String,
    @Expose val firstStacks: List<String>,
    @Expose val brand: String = "",
    @Expose val flagged: Boolean = false,
) {
    fun getIdPair() = name to flagged
}
