package at.hannibal2.skyhanni.data.git.commit

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class CommitTree(
    @Expose val sha: String,
    @Expose val url: String,
    @Expose @field:SerializedName("html_url") val htmlUrl: String? = null,
)
