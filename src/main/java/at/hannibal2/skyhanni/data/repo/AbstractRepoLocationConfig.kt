package at.hannibal2.skyhanni.data.repo

interface AbstractRepoLocationConfig {
    var user: String
    var repoName: String
    var branch: String

    val valid get() = user.isNotEmpty() && repoName.isNotEmpty() && branch.isNotEmpty()

    val defaultUser: String
    val defaultRepoName: String
    val defaultBranch: String

    private fun hasDefaultUser() = user.equals(defaultUser, ignoreCase = true)
    private fun hasDefaultRepoName() = repoName.equals(defaultRepoName, ignoreCase = true)
    private fun hasDefaultBranch() = branch.equals(defaultBranch, ignoreCase = true)

    fun hasDefaultSettings() = hasDefaultUser() && hasDefaultRepoName() && hasDefaultBranch()
    fun reset() {
        user = defaultUser
        repoName = defaultRepoName
        branch = defaultBranch
    }
}
