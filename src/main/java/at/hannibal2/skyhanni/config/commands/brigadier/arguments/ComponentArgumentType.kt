package at.hannibal2.skyhanni.config.commands.brigadier.arguments

import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.escapeDoubleQuote
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.readGreedyString
import at.hannibal2.skyhanni.utils.json.SkyHanniTypeAdapters
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.minecraft.network.chat.Component

class ComponentArgumentType(
    private val allowPlainText: Boolean,
) : ArgumentType<Component> {
    override fun parse(reader: StringReader): Component {
        val input = reader.readGreedyString().escapeDoubleQuote().trim()

        val looksJson =
            input.length >= 2 &&
                (input.startsWith("{") || input.startsWith("["))

        if (!looksJson && allowPlainText) {
            return Component.literal(input)
        }

        val jsonElement = try {
            JsonParser.parseString(input)
        } catch (_: Exception) {
            throw INVALID_JSON.create()
        }

        val result = try {
            adapter.fromJsonTree(jsonElement)
        } catch (_: Exception) {
            null
        }

        return result ?: throw INVALID_COMPONENT.create()
    }

    companion object {

        fun component(allowPlainText: Boolean = false) = ComponentArgumentType(allowPlainText)

        private val INVALID_JSON =
            SimpleCommandExceptionType(Component.literal("Invalid JSON"))

        private val INVALID_COMPONENT =
            SimpleCommandExceptionType(Component.literal("Invalid component"))

        @Suppress("UNCHECKED_CAST")
        private val adapter: TypeAdapter<Component> by lazy {
            SkyHanniTypeAdapters.COMPONENT.adapter as TypeAdapter<Component>
        }
    }
}
