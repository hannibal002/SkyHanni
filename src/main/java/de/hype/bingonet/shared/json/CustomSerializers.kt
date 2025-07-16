package at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.shared.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.awt.Color
import java.lang.reflect.Type
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class InstantSerializer : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Instant {
        if (json.isJsonObject) {
            val jsonObject = json.asJsonObject
            val epochSecond = jsonObject.get("epochSecond").asLong
            val nanoAdjustment = jsonObject.get("nanoAdjustment").asInt
            return Instant.ofEpochSecond(epochSecond, nanoAdjustment.toLong())
        } else {
            val epochMilli = json.asLong
            return Instant.ofEpochMilli(epochMilli)
        }
    }

    override fun serialize(src: Instant, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("epochSecond", src.epochSecond)
        jsonObject.addProperty("nanoAdjustment", src.nano)
        jsonObject.addProperty("toString", src.atZone(ZoneId.of("UTC")).toString())
        return jsonObject
    }
}

class ColorSerializer : JsonSerializer<Color>, JsonDeserializer<Color> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Color {
        val jsonObject = json.asJsonObject
        val red = jsonObject.getAsJsonPrimitive("r").asInt
        val green = jsonObject.getAsJsonPrimitive("g").asInt
        val blue = jsonObject.getAsJsonPrimitive("b").asInt
        val alpha = jsonObject.getAsJsonPrimitive("a").asInt

        return Color(red, green, blue, alpha)
    }

    override fun serialize(src: Color, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("r", src.red)
        jsonObject.addProperty("g", src.green)
        jsonObject.addProperty("b", src.blue)
        jsonObject.addProperty("a", src.alpha)

        return jsonObject
    }
}

 class DurationSerializer : JsonSerializer<Duration>, JsonDeserializer<Duration> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Duration {
        if (json.isJsonObject) {
            val jsonObject = json.asJsonObject
            val seconds = jsonObject.get("seconds").asLong
            val nanoAdjustment = jsonObject.get("nanoAdjustment").asInt
            return Duration.ofSeconds(seconds, nanoAdjustment.toLong())
        } else {
            val millis = json.asLong
            return Duration.ofMillis(millis)
        }
    }

    override fun serialize(src: Duration, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("seconds", src.seconds)
        jsonObject.addProperty("nanoAdjustment", src.nano)
        jsonObject.addProperty("toString", formatTime(src))
        return jsonObject
    }

    fun formatTime(src: Duration): String {
        val seconds = src.seconds
        if (seconds == 0L) return "now"
        val prefix = if (seconds > 0) "in %s" else "%s ago"
        val days = (seconds / 86400).toInt()
        val hours = ((seconds % 86400) / 3600).toInt()
        val minutes = ((seconds % 3600) / 60).toInt()
        val secs = (seconds % 60).toInt()
        val sb = StringBuilder()
        if (days != 0) sb.append(days).append("d ")
        if (hours != 0) sb.append(hours).append("h ")
        if (minutes != 0) sb.append(minutes).append("m ")
        if (secs != 0) sb.append(secs).append("s")
        return String.format(prefix, sb.toString())
    }
}
