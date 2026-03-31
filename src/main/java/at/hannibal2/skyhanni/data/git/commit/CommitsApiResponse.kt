package at.hannibal2.skyhanni.data.git.commit

import at.hannibal2.skyhanni.utils.KSerializable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@KSerializable
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
