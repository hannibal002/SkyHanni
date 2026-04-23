package at.hannibal2.skyhanni.data.git.commit

import com.google.gson.annotations.Expose

data class CommitStats(
    @Expose val total: Long,
    @Expose val additions: Long,
    @Expose val deletions: Long,
)
