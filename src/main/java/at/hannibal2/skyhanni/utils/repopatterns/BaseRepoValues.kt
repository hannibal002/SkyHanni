package at.hannibal2.skyhanni.utils.repopatterns

import at.hannibal2.skyhanni.test.command.ErrorManager
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Base interface for all repository values.
 */
interface RepoValue<R, C> : ReadOnlyProperty<Any?, C> {
    val key: String
    val value: C
    val isLoadedRemotely: Boolean
    val wasOverridden: Boolean
    val parent: RepoPatternKeyOwner?
    val shares: Boolean

    fun loadFromRemote(remoteData: Map<String, String>, forceLocal: Boolean)
    fun dump(): Map<String, String>
}

/**
 * Intermediate abstract class that handles Kotlin property delegation and exclusivity locking.
 */
abstract class AbstractRepoValue<R, C>(
    override val key: String,
    override val parent: RepoPatternKeyOwner?,
    override val shares: Boolean
) : RepoValue<R, C> {

    /**
     * Whether the pattern has obtained a lock on a code location and a key.
     * Once set, no other code locations can access this repo pattern (and therefore the key).
     * @see RepoPatternManager.checkExclusivity
     */
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
        hasObtainedLock = true
        val owner = RepoPatternKeyOwner(thisRef?.javaClass, property, shares, parent)
        if (shares) {
            RepoPatternManager.checkExclusivity(owner, key)
        } else {
            RepoPatternManager.checkNameSpaceExclusivity(owner, key)
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
    shares: Boolean = true // Single values usually share their namespace
) : AbstractRepoValue<String, C>(key, parent, shares) {

    override var isLoadedRemotely = false
        protected set
    override var wasOverridden = false
        protected set

    private var _value: C? = null

    override val value: C
        get() = _value ?: parse(defaultRaw).also { _value = it }

    abstract fun parse(raw: String): C

    override fun loadFromRemote(remoteData: Map<String, String>, forceLocal: Boolean) {
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
    shares: Boolean = false // Lists/Arrays own their namespace prefix, so they don't share by default
) : AbstractRepoValue<List<String>, List<C>>(key, parent, shares) {

    override var isLoadedRemotely = false
        protected set
    override var wasOverridden = false
        protected set

    private var _value: List<C>? = null

    override val value: List<C>
        get() = _value ?: defaultRaw.map { parse(it) }.also { _value = it }

    abstract fun parse(raw: String): C

    override fun loadFromRemote(remoteData: Map<String, String>, forceLocal: Boolean) {
        val prefix = "$key."
        val remoteEntries = remoteData.filterKeys { it.startsWith(prefix) }

        if (!forceLocal && remoteEntries.isNotEmpty()) {
            isLoadedRemotely = true
            val parsedList = remoteEntries.toSortedMap().values.mapNotNull {
                runCatching { parse(it) }.onFailure { e ->
                    ErrorManager.logErrorWithData(e, "Failed to parse remote value for list $key", "remote" to it)
                }.getOrNull()
            }
            wasOverridden = parsedList.map { it.toString() } != defaultRaw.map { parse(it).toString() }
            _value = parsedList.ifEmpty { defaultRaw.map { parse(it) } }
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
