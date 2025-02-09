package at.hannibal2.skyhanni.utils.json

import at.hannibal2.skyhanni.SkyHanniMod
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/*
    Instead of crashing on a wrong value in the config we set the value to null and log a warning.
    This prevents user's config from resetting to default values.
    Which is especially important for when people downgrade their mod version, either on purpose or by accident.
    This does not always work, and can cause a crash later on, but the full config reset is avoided.
 */
object SkippingTypeAdapterFactory : TypeAdapterFactory {

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T> {
        return SafeTypeAdapter(gson.getDelegateAdapter(this, type))
    }

    private class SafeTypeAdapter<T>(val parent: TypeAdapter<T>) : TypeAdapter<T>() {
        override fun write(writer: JsonWriter, value: T) {
            parent.write(writer, value)
        }

        override fun read(reader: JsonReader): T? {
            return try {
                parent.read(reader)
            } catch (e: Exception) {
                val foundValue = reader.peek().toString()
                val message = "Skipping reading JSON from failed path '${reader.path}', found value of '$foundValue'."
                SkyHanniMod.logger.warn(message, e)
                if (!reader.hasNext()) return null
                // reader skip value seems to have an infinite loop if you dont have another element
                reader.skipValue()

                null
            }
        }
    }

//     private fun JsonReader.getPath(usePreviousPath: Boolean): String {
//         val result = StringBuilder().append('$')
//         for (i in 0 until stackSize) {
//             val scope: Int = stack.get(i)
//             when (scope) {
//                 JsonScope.EMPTY_ARRAY, JsonScope.NONEMPTY_ARRAY -> {
//                     var pathIndex: Int = pathIndices.get(i)
//                     // If index is last path element it points to next array element; have to decrement
//                     if (usePreviousPath && pathIndex > 0 && i == stackSize - 1) {
//                         pathIndex--
//                     }
//                     result.append('[').append(pathIndex).append(']')
//                 }
//
//                 JsonScope.EMPTY_OBJECT, JsonScope.DANGLING_NAME, JsonScope.NONEMPTY_OBJECT -> {
//                     result.append('.')
//                     if (pathNames.get(i) != null) {
//                         result.append(pathNames.get(i))
//                     }
//                 }
//
//                 JsonScope.NONEMPTY_DOCUMENT, JsonScope.EMPTY_DOCUMENT, JsonScope.CLOSED -> {}
//                 else -> throw AssertionError("Unknown scope value: $scope")
//             }
//         }
//         return result.toString()
//     }

    // copied over from GSON
    private object JsonScope {
        /** An array with no elements requires no separator before the next element.  */
        const val EMPTY_ARRAY: Int = 1

        /** An array with at least one value requires a separator before the next element.  */
        const val NONEMPTY_ARRAY: Int = 2

        /** An object with no name/value pairs requires no separator before the next element.  */
        const val EMPTY_OBJECT: Int = 3

        /** An object whose most recent element is a key. The next element must be a value.  */
        const val DANGLING_NAME: Int = 4

        /** An object with at least one name/value pair requires a separator before the next element.  */
        const val NONEMPTY_OBJECT: Int = 5

        /** No top-level value has been started yet.  */
        const val EMPTY_DOCUMENT: Int = 6

        /** A top-level value has already been started.  */
        const val NONEMPTY_DOCUMENT: Int = 7

        /** A document that's been closed and cannot be accessed.  */
        const val CLOSED: Int = 8
    }

}
