package at.hannibal2.skyhanni.data.repo

abstract class AbstractRepoLocationConfig {
    abstract var user: String
    abstract var repoName: String
    abstract var branch: String

    val valid get() = user.isNotEmpty() && repoName.isNotEmpty() && branch.isNotEmpty()

    abstract val defaultUser: String
    abstract val defaultRepoName: String
    abstract val defaultBranch: String

    private fun hasDefaultUser() = user.lowercase() == defaultUser.lowercase()
    private fun hasDefaultRepoName() = repoName.lowercase() == defaultRepoName.lowercase()
    private fun hasDefaultBranch() = branch.lowercase() == defaultBranch.lowercase()

    fun hasDefaultSettings() = hasDefaultUser() && hasDefaultRepoName() && hasDefaultBranch()
    fun reset() {
        user = defaultUser
        repoName = defaultRepoName
        branch = defaultBranch
    }
}
