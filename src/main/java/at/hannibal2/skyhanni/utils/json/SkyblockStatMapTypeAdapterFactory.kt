package at.hannibal2.skyhanni.utils.json

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.model.SkyblockStat
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.lang.reflect.ParameterizedType

/*
    Drops map entries keyed by a stat name that no longer exists, as well as SkyblockStat.UNKNOWN itself.
    Reading removed stats as UNKNOWN instead rewrites the stat under that name on the next save, and makes the
    read of the entire file fail with a duplicate key error as soon as a second removed stat joins it.
 */
object SkyblockStatMapTypeAdapterFactory : TypeAdapterFactory {

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != Map::class.java) return null
        val arguments = (type.type as? ParameterizedType)?.actualTypeArguments ?: return null
        if (arguments.first() != SkyblockStat::class.java) return null
        @Suppress("UNCHECKED_CAST")
        return Adapter(gson.getAdapter(TypeToken.get(arguments[1]))).nullSafe() as TypeAdapter<T>
    }

    private class Adapter<V>(private val valueAdapter: TypeAdapter<V>) : TypeAdapter<Map<SkyblockStat, V>>() {

        override fun write(writer: JsonWriter, value: Map<SkyblockStat, V>) {
            writer.beginObject()
            for ((stat, statValue) in value) {
                if (stat == SkyblockStat.UNKNOWN) continue
                writer.name(stat.name.lowercase())
                valueAdapter.write(writer, statValue)
            }
            writer.endObject()
        }

        override fun read(reader: JsonReader): Map<SkyblockStat, V> = LinkedHashMap<SkyblockStat, V>().apply {
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                val stat = SkyblockStat.getValueOrNull(name.uppercase())?.takeIf { it != SkyblockStat.UNKNOWN }
                if (stat == null) {
                    SkyHanniMod.logger.warn("Dropping unknown stat '$name' while reading JSON")
                    reader.skipValue()
                    continue
                }
                put(stat, valueAdapter.read(reader))
            }
            reader.endObject()
        }
    }
}
