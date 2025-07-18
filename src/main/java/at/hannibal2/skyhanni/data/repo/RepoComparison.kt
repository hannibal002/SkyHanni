package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format

data class RepoComparison(
    val localSha: String?,
    val localCommitTime: SimpleTimeMark?,
    val latestSha: String?,
    val latestCommitTime: SimpleTimeMark?,
) {
    val hashesMatch = localSha == latestSha

    fun reportRepoUpToDate() = ChatUtils.clickToClipboard(
        "§7The repo is already up to date!",
        lines = buildList {
            add("latest commit sha: §e$localSha")
            latestCommitTime?.let { latestTime ->
                add("latest commit time: §b$latestTime")
                add("  (§b${latestTime.passedSince().format()} ago§7)")
            }
        },
    )

    fun reportRepoOutdated() = ChatUtils.clickToClipboard(
        "Repo is outdated, updating..",
        lines = buildList {
            add("local commit sha: §e$latestSha")
            localCommitTime?.let { localTime ->
                add("local commit time: §b$localTime")
                add("  (§b${localTime.passedSince().format()} ago§7)")
            }
            add("")
            add("latest commit sha: §e$localSha")
            latestCommitTime?.let { latestTime ->
                add("latest commit time: §b$latestTime")
                add("  (§b${latestTime.passedSince().format()} ago§7)")
                localCommitTime?.let { localTime ->
                    val outdatedDuration = latestTime - localTime
                    add("")
                    add("outdated by: §b${outdatedDuration.format()}")
                }
            }
        },
    )
}
