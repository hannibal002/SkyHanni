package at.hannibal2.skyhanni.utils.repopatterns

import at.hannibal2.skyhanni.SkyHanniMod
import org.intellij.lang.annotations.Language
import java.util.regex.Pattern
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

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
interface RepoPattern : ReadOnlyProperty<Any?, Pattern> {
    /**
     * Check whether [value] has been loaded remotely or from the fallback value at [defaultPattern]. In case this is
     * accessed off-thread there are no guarantees for the correctness of this value in relation to any specific call
     * to [value].
     */
    val isLoadedRemotely: Boolean

    /**
     * Check whether [value] was compiled from a value other than the [defaultPattern]. This is `false` even when
     * loading remotely if the remote pattern matches the local one.
     */
    val wasOverridden: Boolean

    /**
     * The default pattern that is specified at compile time. This local pattern will be a fallback in case there is no
     * remote pattern available or the remote pattern does not compile.
     */
    val defaultPattern: String

    /**
     * Key for this pattern. Used as an identifier when loading from the repo. Should be consistent accross versions.
     */
    val key: String

    /**
     * Should not be accessed directly. Instead, use delegation at one code location and share the regex from there.
     * ```kt
     * val actualValue by pattern
     * ```
     */
    val value: Pattern

    override fun getValue(thisRef: Any?, property: KProperty<*>): Pattern {
        return value
    }


    companion object {
        /**
         * Obtain a reference to a [Pattern] backed by either a local regex, or a remote regex.
         * Check the documentation of [RepoPattern] for more information.
         */
        fun pattern(key: String, @Language("RegExp") fallback: String): RepoPattern {
            return RepoPatternManager.of(key, fallback)
        }

        /**
         * Obtains a [RepoPatternGroup] to allow for easier defining [RepoPattern]s with common prefixes.
         */
        fun group(prefix: String): RepoPatternGroup {
            return RepoPatternGroup(prefix)
        }
    }
}
