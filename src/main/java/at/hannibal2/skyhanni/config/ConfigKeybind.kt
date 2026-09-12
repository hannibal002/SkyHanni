package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.utils.InputCode
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping

class ConfigKeybind {
    @Transient private lateinit var keyMappingPath: String
    private var startingKey: Int? = null

    constructor(inputCode: InputCode) : this(inputCode.value)
    constructor() : this(InputCode.UNKNOWN)

    private constructor(input: Int) {
        startingKey = input
    }

    fun bind(path: String) {
        keyMappingPath = path
        startingKey?.let { startKey ->
            val key = if (MouseCompat.isButtonDown(startKey)) {
                InputConstants.Type.MOUSE.getOrCreate(startKey)
            } else {
                InputConstants.Type.KEYSYM.getOrCreate(startKey)
            }
            keyMapping.setKey(key)
        }
    }

    val keyMapping: KeyMapping get() = KeyboardManager.getKeyMapping(keyMappingPath)!!

    override fun toString() = try { keyMapping.key.value.toString() } catch (e: Exception) { startingKey?.toString() ?: InputCode.UNKNOWN.value.toString() }

    /** Returns the integer keycode currently represented by this config keybind.
     *  If not yet bound, returns the configured starting key (if present) or UNKNOWN.
     */
    val value: Int get() = try {
        keyMapping.key.value
    } catch (e: Exception) {
        startingKey ?: InputCode.UNKNOWN.value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return when (other) {
            is ConfigKeybind -> this.value == other.value
            is InputCode -> this.value == other.value
            is Int -> this.value == other
            else -> false
        }
    }

    override fun hashCode(): Int = value

    companion object {
        val typeAdapter = object : TypeAdapter<ConfigKeybind>() {
            override fun write(out: JsonWriter, value: ConfigKeybind?) {
                if (value == null) {
                    out.nullValue()
                    return
                }
                out.value(value.keyMapping.key.value)
            }

            override fun read(reader: JsonReader): ConfigKeybind = ConfigKeybind(reader.nextInt())
        }
    }
}
