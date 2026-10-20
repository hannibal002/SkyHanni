package at.hannibal2.skyhanni.data.git.commit

import com.google.gson.annotations.Expose

data class Commit(
    @Expose val committer: ShortCommitAuthor,
)
