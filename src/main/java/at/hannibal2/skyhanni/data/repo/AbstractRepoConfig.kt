package at.hannibal2.skyhanni.data.repo

abstract class AbstractRepoConfig<LC : AbstractRepoLocationConfig> {
    abstract var repoAutoUpdate: Boolean
    abstract val updateRepo: Runnable
    abstract val location: LC
}
