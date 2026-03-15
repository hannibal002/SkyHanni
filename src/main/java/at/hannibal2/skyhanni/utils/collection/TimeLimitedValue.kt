package at.hannibal2.skyhanni.utils.collection

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration

class TimeLimitedValue<V : Any>(
    private val expireAfterWrite: Duration,
    private val compute: () -> V?,
) {
    private var lastComputed: SimpleTimeMark = SimpleTimeMark.farPast()
    private var cached: V? = null

    fun get(): V? {
        if (lastComputed.passedSince() > expireAfterWrite) {
            cached = compute()
            lastComputed = SimpleTimeMark.now()
        }
        return cached
    }
}
