package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class LaunchersJson(
    @Expose val launchers: List<LauncherEntry>
)

data class LauncherEntry(
    @Expose val name: String,
    @Expose val firstStacks: List<String>,
    @Expose val flagged: Boolean = false,
)
