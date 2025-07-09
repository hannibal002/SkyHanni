package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import com.google.gson.annotations.SerializedName
import java.io.File
import java.time.Instant

object GitHubUtils {

    abstract class GenericRepoLocationConfig() {
        abstract var user: String
        abstract var name: String
        abstract var branch: String
    }

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
        constructor(config: GenericRepoLocationConfig, shouldError: Boolean = false): this(
            config.user,
            config.name,
            config.branch,
            shouldError,
        )

        private var _internalSha: String? = null
        val sha: String? get() = _internalSha?.takeIf { it.isNotBlank() } ?: run {
            SkyHanniMod.launchIOCoroutine {
                _internalSha = getLatestCommit(!shouldError)?.sha ?: run {
                    if (shouldError) ErrorManager.skyHanniError("Could not fetch latest commit SHA for $user/$repo/$branch")
                    null
                }
            }
            _internalSha
        }
        val apiName = "GitHub - $user/$repo/$branch"
        val commitApiUrl: String = "https://api.github.com/repos/$user/$repo/commits/$branch"

        suspend fun getLatestCommit(silentError: Boolean = true): CommitsApiResponse? {
            val jsonResponse = ApiUtils.getJSONResponse(commitApiUrl, apiName, silentError) ?: return null
            return ConfigManager.gson.fromJson(jsonResponse, CommitsApiResponse::class.java)
        }

        suspend fun downloadCommitZipToFile(destinationZip: File, shaOverride: String? = null): Boolean {
            val shaToUse = shaOverride ?: sha ?: run {
                if (shouldError) ErrorManager.skyHanniError("Cannot get full archive URL without a valid SHA")
                return false
            }
            val fullArchiveUrl = "https://github.com/$user/$repo/archive/$shaToUse.zip"
            return try {
                ApiUtils.getZIPResponse(destinationZip, fullArchiveUrl, apiName, !shouldError)
                true
            } catch (e: Exception) {
                SkyHanniMod.logger.error("Failed to download archive from $fullArchiveUrl", e)
                false
            }
        }
    }

    @KSerializable
    data class CommitsApiResponse(
        val sha: String,
        @SerializedName("node_id") val nodeId: String,
        val commit: Commit,
        val url: String,
        @SerializedName("html_url") val htmlUrl: String,
        @SerializedName("comments_url") val commentsUrl: String,
        val author: CommitAuthor,
        val committer: CommitAuthor,
        val parents: List<CommitTree>,
        val stats: CommitStats,
        val files: List<CommitFile>,
    )

    data class Commit(
        val author: ShortCommitAuthor,
        val committer: ShortCommitAuthor,
        val message: String,
        val tree: CommitTree,
        val url: String,
        @SerializedName("comment_count") val commentCount: Int,
        val verification: CommitVerification,
    )

    @KSerializable
    data class ShortCommitAuthor(
        val name: String,
        val email: String,
        @SerializedName("date") private val dateString: String,
    ) {
        val date: SimpleTimeMark get() = Instant.parse(dateString).toEpochMilli().asTimeMark()
    }

    @KSerializable
    data class CommitAuthor(
        val login: String,
        val id: Int,
        @SerializedName("node_id") val nodeId: String,
        @SerializedName("avatar_url") val avatarUrl: String,
        @SerializedName("gravatar_id") val gravatarId: String,
        val url: String,
        @SerializedName("html_url") val htmlUrl: String,
        @SerializedName("followers_url") val followersUrl: String,
        @SerializedName("following_url") val followingUrl: String,
        @SerializedName("gists_url") val gistsUrl: String,
        @SerializedName("starred_url") val starredUrl: String,
        @SerializedName("subscriptions_url") val subscriptionsUrl: String,
        @SerializedName("organizations_url") val organizationsUrl: String,
        @SerializedName("repos_url") val reposUrl: String,
        @SerializedName("events_url") val eventsUrl: String,
        @SerializedName("received_events_url") val receivedEventsUrl: String,
        val type: String,
        @SerializedName("user_view_type") val userViewType: String,
        @SerializedName("site_admin") val siteAdmin: Boolean,
    )

    @KSerializable
    data class CommitTree(
        val sha: String,
        val url: String,
        @SerializedName("html_url") val htmlUrl: String? = null,
    )

    @KSerializable
    data class CommitVerification(
        val verified: Boolean,
        val reason: String,
        val signature: String? = null,
        val payload: String? = null,
        @SerializedName("verified_at") private val verifiedAtString: String? = null,
    ) {
        val verifiedAt: SimpleTimeMark? get() = verifiedAtString?.let {
            Instant.parse(it).toEpochMilli().asTimeMark()
        }
    }

    data class CommitStats(
        val total: Long,
        val additions: Long,
        val deletions: Long,
    )

    data class CommitFile(
        val sha: String,
        val filename: String,
        val status: String,
        val additions: Int,
        val deletions: Int,
        val changes: Int,
        @SerializedName("blob_url") val blobUrl: String,
        @SerializedName("raw_url") val rawUrl: String,
        @SerializedName("contents_url") val contentsUrl: String,
        @SerializedName("patch") val patch: String,
    )

}
