package at.hannibal2.skyhanni.utils.repopatterns

import org.intellij.lang.annotations.Language

/**
 * A utility class for allowing easier definitions of [RepoPattern]s with a common prefix.
 */
class RepoPatternGroup internal constructor(val prefix: String) {
    init {
        RepoPatternManager.verifyKeyShape(prefix)
    }

    /**
     * Shortcut to [RepoPattern.pattern] prefixed with [prefix].
     */
    fun pattern(key: String, @Language("RegExp") fallback: String): RepoPattern {
        return RepoPattern.pattern("$prefix.$key", fallback)
    }

    /**
     * Shortcut to [RepoPattern.group] prefixed with [prefix].
     */
    fun group(subgroup: String): RepoPatternGroup {
        return RepoPatternGroup("$prefix.$subgroup")
    }
}
