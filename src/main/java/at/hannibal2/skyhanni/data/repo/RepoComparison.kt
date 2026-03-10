package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format

data class RepoComparison(
    val repoName: String,
    val localSha: String?,
    val localCommitTime: SimpleTimeMark?,
    val latestSha: String?,
    val latestCommitTime: SimpleTimeMark?,
) {
    constructor(
        repoName: String,
        localCommit: RepoCommit?,
        latestCommit: RepoCommit?,
    ) : this(
        repoName = repoName,
        localSha = localCommit?.sha,
        localCommitTime = localCommit?.time,
        latestSha = latestCommit?.sha,
        latestCommitTime = latestCommit?.time,
    )

    val local: RepoCommit by lazy { RepoCommit(localSha, localCommitTime) }
    val latest: RepoCommit by lazy { RepoCommit(latestSha, latestCommitTime) }

    val hashesMatch = localSha == latestSha

    fun reportRepoUpToDate() = ChatUtils.clickToClipboard(
        "§7The $repoName repo is already up to date!",
        lines = buildList {
            add("Repo: $repoName")
            add("")
            add("latest commit sha: §e$localSha")
            latestCommitTime?.let { latestTime ->
                add("latest commit time: §b$latestTime")
                add("  (§b${latestTime.passedSince().format()} ago§7)")
            }
        },
    )

    fun reportForceRebuild() = reportRepoOutdated("Force redownloading repo..")

    fun reportRepoOutdated(
        mainMessage: String = "$repoName Repo is outdated, updating..",
    ) = ChatUtils.clickToClipboard(
        mainMessage,
        lines = buildList {
            add("Repo: $repoName")
            add("")
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
                if (localSha == latestSha) return@buildList
                localCommitTime?.let { localTime ->
                    val outdatedDuration = latestTime - localTime
                    add("")
                    add("outdated by: §b${outdatedDuration.format()}")
                }
            }
        },
    )
}
