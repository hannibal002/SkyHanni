package at.hannibal2.skyhanni.data.git.commit

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

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
