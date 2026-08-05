package at.hannibal2.skyhanni.utils.repopatterns

import org.intellij.lang.annotations.Language
import java.util.regex.Pattern

class RepoString private constructor(key: String, fallback: String, parent: RepoPatternKeyOwner? = null) :
    BaseSingleRepoValue<String>(key, fallback, parent) {

    override fun parse(raw: String): String = raw

    companion object {
        /** Factory method for a single String */
        fun string(key: String, fallback: String, parent: RepoPatternKeyOwner? = null): RepoString {
            return RepoString(key, fallback, parent)
        }

        /** Factory method for a List of Strings */
        fun list(key: String, vararg fallbacks: String, parent: RepoPatternKeyOwner? = null): RepoStringList {
            return RepoStringList(key, fallbacks.toList(), parent)
        }
    }
}

class RepoStringList internal constructor(key: String, fallbacks: List<String>, parent: RepoPatternKeyOwner? = null) :
    BaseListRepoValue<String>(key, fallbacks, parent) {
    override fun parse(raw: String): String = raw
}

class RepoPattern private constructor(key: String, fallback: String, parent: RepoPatternKeyOwner? = null) :
    BaseSingleRepoValue<Pattern>(key, fallback, parent) {

    override fun parse(raw: String): Pattern = Pattern.compile(raw)

    companion object {
        /**
         * Obtain a reference to a [Pattern] backed by either a local regex, or a remote regex.
         * Check the documentation of [RepoPattern] for more information.
         *
         * This method supports "Open regex101.com" using [LivePlugin](https://plugins.jetbrains.com/plugin/7282-liveplugin).
         * To use it, install LivePlugin, enable "Run plugins on IDE start" and "Run project specific plugins".
         * Now you can use ALT+ENTER while hovering over a [pattern] call using your text cursor to access the "Open in regex101.com" intention.
         * Add a KDoc comment to the associated variable containing lines starting with `REGEX-TEST: ` to pre-fill examples.
         */
        fun pattern(key: String, @Language("RegExp") fallback: String, parent: RepoPatternKeyOwner? = null): RepoPattern {
            return RepoPattern(key, fallback, parent)
        }

        fun list(key: String, @Language("RegExp") vararg fallbacks: String, parent: RepoPatternKeyOwner? = null): RepoPatternList {
            return RepoPatternList(key, fallbacks.toList(), parent)
        }

        /**
         * Obtains a [RepoPatternGroup] to allow for easier defining [RepoPattern]s with common prefixes.
         */
        fun group(prefix: String): RepoPatternGroup {
            return RepoPatternGroup(prefix)
        }

        /**
         * Obtains a [RepoPatternExclusiveGroup] which functions like a [RepoPatternGroup] but the key namespace can only be used via this object.
         */
        fun exclusiveGroup(prefix: String): RepoPatternExclusiveGroupInfo {
            return RepoPatternExclusiveGroupInfo(prefix, null)
        }
    }
}

class RepoPatternList internal constructor(key: String, fallbacks: List<String>, parent: RepoPatternKeyOwner? = null) :
    BaseListRepoValue<Pattern>(key, fallbacks, parent) {
    override fun parse(raw: String): Pattern = Pattern.compile(raw)
}
