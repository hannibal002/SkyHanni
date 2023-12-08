package at.hannibal2.skyhanni.utils.repopatterns

import java.util.regex.Pattern
import kotlin.reflect.KProperty

/**
 * Internal class implementing [RepoPattern]. Obtain via [RepoPattern.pattern].
 */
class RepoPatternImpl(
    override val defaultPattern: String,
    override val key: String,
) : RepoPattern {
    var compiledPattern: Pattern = Pattern.compile(defaultPattern)
    var wasLoadedRemotely = false
    override var wasOverridden = false

    /**
     * Whether the pattern has obtained a lock on a code location and a key.
     * Once set, no other code locations can access this repo pattern (and therefore the key).
     * @see RepoPatternManager.checkExclusivity
     */
    var hasObtainedLock = false

    override fun getValue(thisRef: Any?, property: KProperty<*>): Pattern {
        verifyLock(thisRef, property)
        return super.getValue(thisRef, property)
    }

    /**
     * Try to lock the [key] to this key location.
     * @see RepoPatternManager.checkExclusivity
     */
    fun verifyLock(thisRef: Any?, property: KProperty<*>) {
        if (hasObtainedLock) return
        hasObtainedLock = true
        val owner = RepoPatternKeyOwner(thisRef?.javaClass, property)
        RepoPatternManager.checkExclusivity(owner, key)
    }


    override val value: Pattern
        get() {
            return compiledPattern
        }
    override val isLoadedRemotely: Boolean
        get() {
            return wasLoadedRemotely
        }
}
