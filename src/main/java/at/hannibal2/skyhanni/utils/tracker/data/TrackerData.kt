package at.hannibal2.skyhanni.utils.tracker.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.storage.Resettable
import at.hannibal2.skyhanni.utils.Stopwatch
import at.hannibal2.skyhanni.utils.tracker.GardenSession
import at.hannibal2.skyhanni.utils.tracker.NormalSession
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class TrackerData<T : SessionUptime> : Resettable {

    // Resolve the SessionUptime subtype from the generic parameter at runtime.
    // Type erasure means we cannot use T::class directly; the concrete subclass carries the
    // type argument in its superclass signature, which ParameterizedType exposes via reflection.
    @Suppress("UNCHECKED_CAST")
    private val uptimeClass: KClass<T> by lazy {
        val genericSuper = this.javaClass.genericSuperclass as ParameterizedType
        val jClass = genericSuper.actualTypeArguments[0] as Class<T>
        jClass.kotlin
    }

    // Gson may deserialize null keys or null values from saves written by older builds that
    // had bugs or incomplete migration. Storing with nullable types absorbs those entries;
    // the non-nullable cast below is safe after migrateData() removes all nulls.
    @SerializedName("sessionUptime")
    private val sessionUptimeInternal: MutableMap<SessionUptime?, Stopwatch?> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    private val sessionUptime: MutableMap<SessionUptime, Stopwatch>
        get() = sessionUptimeInternal as MutableMap<SessionUptime, Stopwatch>

    @Expose
    private var migrated = false

    private var activeSession: SessionUptime? = null

    init {
        addSessionUptime()
    }

    private fun addSessionUptime() = when (uptimeClass) {
        SessionUptime.Normal::class -> NormalSession.entries.forEach { session ->
            sessionUptime[SessionUptime.Normal(session)] = Stopwatch()
        }
        SessionUptime.Garden::class -> GardenSession.entries.forEach { session ->
            sessionUptime[SessionUptime.Garden(session)] = Stopwatch()
        }
        else -> {}
    }

    override fun reset() {
        super.reset()
        sessionUptime.values.forEach { it.reset() }
    }

    fun getSessionMap() = sessionUptime.toMap()

    fun getActiveStopwatch(): Stopwatch? {
        val active = activeSession
        return active?.let { sessionUptime.getOrPut(active) { Stopwatch() } } ?: run {
            activeSession = sessionUptime.keys.firstOrNull()
            sessionUptime.getOrPut(activeSession ?: return null) { Stopwatch() }
        }
    }

    fun setActiveStopwatch(session: SessionUptime, swapExtraTime: Boolean) {
        if (session != activeSession) {
            val duration = getActiveStopwatch()?.pause(revertLap = swapExtraTime)
            activeSession = session
            if (swapExtraTime) getActiveStopwatch()?.add(duration ?: 0.seconds)
        }
        getActiveStopwatch()?.start(true)
    }

    fun getTotalUptime(): Duration {
        if (!migrated) migrateData()
        val entries = if (uptimeClass == SessionUptime.Garden::class) {
            sessionUptime.entries.filter { SkyHanniMod.feature.garden.trackerUptimeSettings.types.get().contains(it.key.garden) }
        } else sessionUptime.entries

        var uptime = Duration.ZERO
        entries.forEach { entry ->
            uptime += entry.value.getDuration()
        }
        return uptime
    }

    private fun migrateData() {
        migrated = true
        // Remove null keys and values that may have been written by older builds.
        sessionUptimeInternal.entries.removeAll { it.key == null || it.value == null }

        when (uptimeClass) {
            SessionUptime.Normal::class -> filterAndRemove(uptimeClass, SessionUptime.Normal(NormalSession.NORMAL))
            SessionUptime.Garden::class -> filterAndRemove(uptimeClass, SessionUptime.Garden(GardenSession.UNKNOWN))
        }
    }

    /**
     * Merges any entries whose key is not an instance of [entryType] into [migratedSessionType].
     *
     * Preserves accumulated durations from saves written by older builds that stored session
     * types differently, rather than silently discarding them.
     */
    private fun filterAndRemove(entryType: KClass<out SessionUptime>, migratedSessionType: SessionUptime) {
        val entries = sessionUptime.entries.filter { entry ->
            !entryType.isInstance(entry.key)
        }
        if (entries.isEmpty()) return
        entries.forEach { entry ->
            val unknown = sessionUptime.getOrPut(migratedSessionType) { Stopwatch() }
            unknown.add(entry.value.getDuration())
            sessionUptime.remove(entry.key)
        }
    }
}
