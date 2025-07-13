package at.hannibal2.skyhanni.data.repo

abstract class AbstractRepoConfig<RLC : AbstractRepoLocationConfig> {
    abstract var repoAutoUpdate: Boolean
    abstract val updateRepo: Runnable
    abstract val location: RLC
    abstract var unzipToMemory: Boolean
}
