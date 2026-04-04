package at.hannibal2.skyhanni.data.repo

interface AbstractRepoConfig {
    var repoAutoUpdate: Boolean
    val updateRepo: Runnable
    val location: AbstractRepoLocationConfig
    var unzipToMemory: Boolean
}
