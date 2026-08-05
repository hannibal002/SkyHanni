package at.hannibal2.skyhanni.utils.repopatterns

import org.intellij.lang.annotations.Language
import java.util.regex.Pattern

/**
 * RepoPattern is our innovative tool to cope with the fucking game updates Hypixel deems to be important enough to brick
 * our regexes over for.
 *
 * ## Usage
 *
 * RepoPattern is only available in kotlin. If you must use a regex from java code that you anticipate might need updating
 * in the future, please have a kotlin wrapper from which you pull the regex using a getter method of sorts.
 *
 * In order to use a RepoPattern, you need to obtain a reference to that repo pattern statically during pre init. This
 * means you must either be loaded by [SkyHanniMod.loadModule] directly, or must be loaded during class or object
 * initialization of an object that is pre init loaded. You will then have to bind that repo pattern to a field using
 * kotlin delegation syntax:
 *
 * ```kt
 * class SomeFeatureModule {
 *     // Initialize your regex
 *     val myRegey by /* notice the by here, instead of a = */ RepoPattern.of("someUniqueKey", "^[a-z]+")
 * }
 * ```
 *
 * If used like this, nothing will break. If you do want to live a little more daring, you can also keep the original
 * reference around. If you do this, make sure that you only ever create one RepoPattern per key, and only ever use that
 * RepoPattern instance bound to one field like so:
 * ```kt
 * class SomeFeatureModule {
 *     // Initialize your RepoPattern
 *     val meta = RepoPattern.of("someUniqueKey", "^[a-z]+")
 *     val pattern by meta // Creating a new RepoPattern.of in here for the same key would be illegal
 * }
 * ```
 *
 * When accessing the metaobject (the RepoPattern instance itself), then you afford yourself less protection at the cost
 * of slightly more options.
 */
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
