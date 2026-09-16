package at.hannibal2.skyhanni.data.git.commit

import at.hannibal2.skyhanni.utils.KSerializable
import com.google.gson.annotations.Expose

@KSerializable
data class CommitsApiResponse(
    @Expose val sha: String,
    @Expose val commit: Commit,
)
