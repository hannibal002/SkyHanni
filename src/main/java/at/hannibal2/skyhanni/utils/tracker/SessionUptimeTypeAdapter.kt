package at.hannibal2.skyhanni.utils.tracker

import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

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
