package at.hannibal2.skyhanni.utils.repopatterns

/**
 * A utility class for allowing easier definitions of [RepoPattern]s with a common prefix.
 */
class RepoPatternExclusiveGroup internal constructor(prefix: String, parent: RepoPatternKeyOwner?) :
    RepoPatternGroup(prefix, parent) {

}
