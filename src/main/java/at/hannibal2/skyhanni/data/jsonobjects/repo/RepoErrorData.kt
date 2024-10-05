package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class RepoErrorData(
    @Expose var messageExact: List<String>?,
    @Expose var messageStartsWith: List<String>?,
    @Expose var replaceMessage: String?,
    @Expose var affectedVersions: List<String> = listOf(),
)
