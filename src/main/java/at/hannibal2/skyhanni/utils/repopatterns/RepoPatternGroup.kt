package at.hannibal2.skyhanni.utils.repopatterns

import org.intellij.lang.annotations.Language

/**
 * A utility class for allowing easier definitions of [RepoPattern]s with a common prefix.
 */
open class RepoPatternGroup internal constructor(
    val prefix: String,
    protected var parent: RepoPatternKeyOwner? = null,
) {

    init {
        RepoPatternManager.verifyKeyShape(prefix)
    }

    /**
     * Shortcut to [RepoPattern.pattern] prefixed with [prefix].
     */
    fun pattern(key: String, @Language("RegExp") fallback: String): RepoPattern {
        return RepoPatternManager.of("$prefix.$key", fallback, parent)
    }

    /**
     * Shortcut to [RepoPattern.list] prefixed with [prefix].
     */
    fun list(key: String, @Language("RegExp") vararg fallbacks: String): RepoPatternList {
        return RepoPatternManager.ofList("$prefix.$key", fallbacks, parent)
    }

    /**
     * Shortcut to [RepoPattern.group] prefixed with [prefix].
     */
    fun group(subgroup: String): RepoPatternGroup {
        return RepoPatternGroup("$prefix.$subgroup", parent)
    }

    /**
     * Shortcut to [RepoPattern.exclusiveGroup] prefixed with [prefix].
     */
    fun exclusiveGroup(subgroup: String): RepoPatternExclusiveGroupInfo {
        return RepoPatternExclusiveGroupInfo("$prefix.$subgroup", parent)
    }
}
