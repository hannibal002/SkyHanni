package at.hannibal2.skyhanni.utils.repopatterns

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.StringUtils
import java.util.NavigableMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Base interface for all repository values.
 */
interface RepoValue<R, C> : ReadOnlyProperty<Any?, C> {
    /**
     * Check whether [value] has been loaded remotely or from the fallback value at [defaultRaw]. In case this is
     * accessed off-thread there are no guarantees for the correctness of this value in relation to any specific call
     * to [value].
     */
    val isLoadedRemotely: Boolean

    /**
     * Check whether [value] was compiled from a value other than the [defaultRaw]. This is `false` even when
     * loading remotely if the remote pattern matches the local one.
     */
    val wasOverridden: Boolean

    /**
     * Key for this value. Used as an identifier when loading from the repo. Should be consistent across versions.
     */

    val key: String
    /**
     * Should not be accessed directly. Instead, use delegation at one code location and share the regex from there.
     * ```kt
     * val actualValue: Pattern by pattern
     * ```
     */
    val value: C

    val parent: RepoPatternKeyOwner?
    val shares: Boolean

    fun loadFromRemote(remoteData: NavigableMap<String, String>, forceLocal: Boolean)
    fun dump(): Map<String, String>
}

/**
 * Intermediate abstract class that handles Kotlin property delegation and exclusivity locking.
 */
abstract class AbstractRepoValue<R, C>(
    override val key: String,
    override val parent: RepoPatternKeyOwner?,
    override val shares: Boolean,
) : RepoValue<R, C> {

    /**
     * Whether the pattern has obtained a lock on a code location and a key.
     * Once set, no other code locations can access this repo pattern (and therefore the key).
     * @see RepoPatternManager.checkExclusivity
     */
    @Volatile
    internal var hasObtainedLock = false

    override fun getValue(thisRef: Any?, property: KProperty<*>): C {
        verifyLock(thisRef, property)
        return value
    }

    /**
     * Try to lock the [key] to this key location.
     * @see RepoPatternManager.checkExclusivity
     */
    private fun verifyLock(thisRef: Any?, property: KProperty<*>) {
        if (hasObtainedLock) return
        synchronized(this) {
            if (hasObtainedLock) return
            hasObtainedLock = true
            val owner = RepoPatternKeyOwner(thisRef?.javaClass, property, shares, parent)
            if (shares) {
                RepoPatternManager.checkExclusivity(owner, key)
            } else {
                RepoPatternManager.checkNameSpaceExclusivity(owner, key)
            }
        }
    }
}

/**
 * Base class for a single remote value (like String, Pattern, Int).
 */
abstract class BaseSingleRepoValue<C>(
    key: String,
    val defaultRaw: String,
    parent: RepoPatternKeyOwner? = null,
    shares: Boolean = true, // Single values usually share their namespace
) : AbstractRepoValue<String, C>(key, parent, shares) {

    override var isLoadedRemotely = false
        protected set
    override var wasOverridden = false
        protected set

    private var _value: C? = null

    override val value: C
        get() = _value ?: parse(defaultRaw).also { _value = it }

    abstract fun parse(raw: String): C

    override fun loadFromRemote(remoteData: NavigableMap<String, String>, forceLocal: Boolean) {
        val remoteString = remoteData[key]
        if (!forceLocal && remoteString != null) {
            isLoadedRemotely = true
            wasOverridden = remoteString != defaultRaw
            _value = runCatching { parse(remoteString) }.getOrElse {
                ErrorManager.logErrorWithData(it, "Failed to parse remote value for $key", "remote" to remoteString)
                parse(defaultRaw)
            }
        } else {
            isLoadedRemotely = false
            wasOverridden = false
            _value = parse(defaultRaw)
        }
    }

    override fun dump(): Map<String, String> = mapOf(key to defaultRaw)

    init {
        @Suppress("LeakingThis")
        RepoPatternManager.register(this)
    }
}

/**
 * Base class for a list/array of remote values.
 */
abstract class BaseListRepoValue<C>(
    key: String,
    val defaultRaw: List<String>,
    parent: RepoPatternKeyOwner? = null,
    shares: Boolean = false, // Lists/Arrays own their namespace prefix, so they don't share by default
) : AbstractRepoValue<List<String>, List<C>>(key, parent, shares) {

    override var isLoadedRemotely = false
        protected set
    override var wasOverridden = false
        protected set

    private var _value: List<C>? = null

    override val value: List<C>
        get() = _value ?: defaultRaw.map { parse(it) }.also { _value = it }

    abstract fun parse(raw: String): C

    override fun loadFromRemote(remoteData: NavigableMap<String, String>, forceLocal: Boolean) {
        val prefix = "$key."
        val matchingEntries = StringUtils.subMapOfStringsStartingWith(prefix, remoteData)
        val isPresentRemotely = matchingEntries.isNotEmpty() || remoteData.containsKey(key)

        if (!forceLocal && isPresentRemotely) {
            isLoadedRemotely = true
            val parsedList = matchingEntries.values.mapNotNull { rawValue ->
                runCatching { parse(rawValue) }.onFailure { e ->
                    ErrorManager.logErrorWithData(e, "Failed to parse remote value for list $key", "remote" to rawValue)
                }.getOrNull()
            }
            wasOverridden = parsedList.map { it.toString() } != defaultRaw.map { parse(it).toString() }
            _value = parsedList
        } else {
            isLoadedRemotely = false
            wasOverridden = false
            _value = defaultRaw.map { parse(it) }
        }
    }

    override fun dump(): Map<String, String> {
        return defaultRaw.mapIndexed { index, s -> "$key.$index" to s }.toMap()
    }

    init {
        @Suppress("LeakingThis")
        RepoPatternManager.register(this)
    }
}
