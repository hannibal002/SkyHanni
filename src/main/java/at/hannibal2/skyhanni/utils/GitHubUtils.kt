package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.repo.AbstractRepoLocationConfig
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.api.ApiUtils
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.File
import java.time.Instant

object GitHubUtils {

    /**
     * Represents the location of a GitHub repository.
     * @param user The GitHub username or organization.
     * @param repo The repository name.
     * @param branch The branch name, defaults to "main".
     * @param shouldError If true, will throw an error if the latest commit SHA cannot be fetched, or if the download fails.
     */
    data class RepoLocation(
        val user: String,
        val repo: String,
        val branch: String = "main",
        private val shouldError: Boolean = false,
    ) {
        constructor(config: AbstractRepoLocationConfig, shouldError: Boolean = false) : this(
            config.user,
            config.repoName,
            config.branch,
            shouldError,
        )

        val location = "$user/$repo/$branch"
        private val apiName = "GitHub - $location"
        private val commitApiUrl: String = "https://api.github.com/repos/$user/$repo/commits/$branch"

        suspend fun getLatestCommit(silentError: Boolean = true): CommitsApiResponse? {
            val (_, jsonResponse) = ApiUtils.getJsonResponse(commitApiUrl, apiName, silentError).assertSuccessWithData() ?: run {
                SkyHanniMod.logger.error("Failed to fetch latest commits.")
                return null
            }
            return ConfigManager.gson.fromJson(jsonResponse, CommitsApiResponse::class.java)
        }

        suspend fun downloadCommitZipToFile(destinationZip: File, shaOverride: String? = null): Boolean {
            val shaToUse = shaOverride ?: getLatestCommit(!shouldError)?.sha ?: run {
                if (shouldError) ErrorManager.skyHanniError("Cannot get full archive URL without a valid SHA")
                return false
            }
            val fullArchiveUrl = "https://github.com/$user/$repo/archive/$shaToUse.zip"
            return try {
                if (shouldError) {
                    SkyHanniMod.logger.info("Downloading $shaToUse for $user/$repo/$branch\nUrl: $fullArchiveUrl")
                }
                ApiUtils.getZipResponse(destinationZip, fullArchiveUrl, apiName, !shouldError)
                true
            } catch (e: Exception) {
                SkyHanniMod.logger.error("Failed to download archive from $fullArchiveUrl", e)
                false
            }
        }
    }

    data class CommitsApiResponse(
        @Expose val sha: String,
        @Expose @field:SerializedName("node_id") val nodeId: String,
        @Expose val commit: Commit,
        @Expose val url: String,
        @Expose @field:SerializedName("html_url") val htmlUrl: String,
        @Expose @field:SerializedName("comments_url") val commentsUrl: String,
        @Expose val author: CommitAuthor,
        @Expose val committer: CommitAuthor,
        @Expose val parents: List<CommitTree>,
        @Expose val stats: CommitStats,
        @Expose val files: List<CommitFile>,
    )

    data class Commit(
        @Expose val author: ShortCommitAuthor,
        @Expose val committer: ShortCommitAuthor,
        @Expose val message: String,
        @Expose val tree: CommitTree,
        @Expose val url: String,
        @Expose @field:SerializedName("comment_count") val commentCount: Int,
        @Expose val verification: CommitVerification,
    )

    data class ShortCommitAuthor(
        @Expose val name: String,
        @Expose val email: String,
        @Expose @field:SerializedName("date") private val dateString: String,
    ) {
        val date: SimpleTimeMark get() = Instant.parse(dateString).toEpochMilli().asTimeMark()
    }

    data class CommitAuthor(
        @Expose val login: String,
        @Expose val id: Int,
        @Expose @field:SerializedName("node_id") val nodeId: String,
        @Expose @field:SerializedName("avatar_url") val avatarUrl: String,
        @Expose @field:SerializedName("gravatar_id") val gravatarId: String,
        @Expose val url: String,
        @Expose @field:SerializedName("html_url") val htmlUrl: String,
        @Expose @field:SerializedName("followers_url") val followersUrl: String,
        @Expose @field:SerializedName("following_url") val followingUrl: String,
        @Expose @field:SerializedName("gists_url") val gistsUrl: String,
        @Expose @field:SerializedName("starred_url") val starredUrl: String,
        @Expose @field:SerializedName("subscriptions_url") val subscriptionsUrl: String,
        @Expose @field:SerializedName("organizations_url") val organizationsUrl: String,
        @Expose @field:SerializedName("repos_url") val reposUrl: String,
        @Expose @field:SerializedName("events_url") val eventsUrl: String,
        @Expose @field:SerializedName("received_events_url") val receivedEventsUrl: String,
        @Expose val type: String,
        @Expose @field:SerializedName("user_view_type") val userViewType: String,
        @Expose @field:SerializedName("site_admin") val siteAdmin: Boolean,
    )

    data class CommitTree(
        @Expose val sha: String,
        @Expose val url: String,
        @Expose @field:SerializedName("html_url") val htmlUrl: String? = null,
    )

    data class CommitVerification(
        @Expose val verified: Boolean,
        @Expose val reason: String,
        @Expose val signature: String? = null,
        @Expose val payload: String? = null,
        @Expose @field:SerializedName("verified_at") private val verifiedAtString: String? = null,
    ) {
        val verifiedAt: SimpleTimeMark? get() = verifiedAtString?.let {
            Instant.parse(it).toEpochMilli().asTimeMark()
        }
    }

    data class CommitStats(
        @Expose val total: Long,
        @Expose val additions: Long,
        @Expose val deletions: Long,
    )

    data class CommitFile(
        @Expose val sha: String,
        @Expose val filename: String,
        @Expose val status: String,
        @Expose val additions: Int,
        @Expose val deletions: Int,
        @Expose val changes: Int,
        @Expose @field:SerializedName("blob_url") val blobUrl: String,
        @Expose @field:SerializedName("raw_url") val rawUrl: String,
        @Expose @field:SerializedName("contents_url") val contentsUrl: String,
        @Expose @field:SerializedName("patch") val patch: String,
    )

}
