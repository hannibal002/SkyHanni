package at.hannibal2.skyhanni.data.git.commit

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Commit(
    @Expose val author: ShortCommitAuthor,
    @Expose val committer: ShortCommitAuthor,
    @Expose val message: String,
    @Expose val tree: CommitTree,
    @Expose val url: String,
    @Expose @field:SerializedName("comment_count") val commentCount: Int,
    @Expose val verification: CommitVerification,
)
