package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.utils.Stopwatch
import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.annotations.Expose
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class TrackerData<T : SessionUptime>(
    private val uptimeClass: KClass<T>
) {
    @Expose
    private val sessionUptime: MutableMap<SessionUptime, Stopwatch> = mutableMapOf()

    private var activeSession: SessionUptime? = null

    init {
        addSessionUptime()
        activeSession = sessionUptime.keys.firstOrNull()
    }
    private fun addSessionUptime() {
        when (uptimeClass) {
            SessionUptime.Normal::class -> {
                NormalSession.entries.forEach { session ->
                    sessionUptime[SessionUptime.Normal(session)] = Stopwatch()
                }
            }

            SessionUptime.Garden::class -> {
                GardenSession.entries.forEach { session ->
                    sessionUptime[SessionUptime.Garden(session)] = Stopwatch()
                }
            }
        }
    }

    fun getActiveStopwatch(): Stopwatch? = activeSession?.let { sessionUptime[it] }

    fun setActiveStopwatch(session: SessionUptime) {
        if (session != activeSession) {
            val duration = sessionUptime[activeSession]?.pause(revertLap = true)
            activeSession = session
            sessionUptime[activeSession]?.add(duration ?: 0.seconds)
        }
        sessionUptime[activeSession]?.start(true)
    }

    open fun getTotalUptime(): Duration =
        sessionUptime.values.fold(Duration.ZERO) { acc, stopwatch ->
            acc + stopwatch.getDuration()
        }

    fun reset() {
        for (session in sessionUptime.entries) {
            sessionUptime[session.key]?.reset()
        }
        resetData()
    }

    protected abstract fun resetData()
}

sealed class SessionUptime {
    data class Normal(val sessionType: NormalSession) : SessionUptime()
    data class Garden(val sessionType: GardenSession) : SessionUptime()
}

enum class NormalSession {
    NORMAL,
}

enum class GardenSession {
    PEST,
    VISITOR,
    CROP,
}

class SessionUptimeTypeAdapter : TypeAdapter<SessionUptime>() {
    override fun write(out: JsonWriter, value: SessionUptime) {
        out.beginObject()
        when (value) {
            is SessionUptime.Garden -> {
                out.name("type").value("garden")
                out.name("session").value(value.sessionType.name)
            }
            is SessionUptime.Normal -> {
                out.name("type").value("normal")
                out.name("session").value(value.sessionType.name)
            }
        }
        out.endObject()
    }

    override fun read(reader: JsonReader): SessionUptime {
        var type: String? = null
        var sessionName: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "type" -> type = reader.nextString()
                "session" -> sessionName = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        if (type == null || sessionName == null) {
            throw JsonParseException("Missing required fields: type=$type, session=$sessionName")
        }

        return when (type) {
            "garden" -> SessionUptime.Garden(GardenSession.valueOf(sessionName))
            "normal" -> SessionUptime.Normal(NormalSession.valueOf(sessionName))
            else -> throw JsonParseException("Unknown type: $type")
        }
    }
}

