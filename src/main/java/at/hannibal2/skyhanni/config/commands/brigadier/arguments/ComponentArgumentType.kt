package at.hannibal2.skyhanni.config.commands.brigadier.arguments

import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.readGreedyString
import at.hannibal2.skyhanni.utils.json.SkyHanniTypeAdapters
import com.google.gson.TypeAdapter
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.minecraft.network.chat.Component

class ComponentArgumentType(private val allowPlainText: Boolean) : ArgumentType<Component> {

    override fun parse(reader: StringReader): Component {
        val input = reader.readGreedyString()

        // the length check is to allow stuff like: "[:<"
        val looksJson = input.length > 3 && (input.startsWith("{") || input.startsWith("["))

        if (looksJson || !allowPlainText) {
            try {
                return adapter.fromJson(input)
            } catch (_: Exception) {
                throw INVALID_COMPONENT.create()
            }
        }

        return Component.literal(input)
    }

    companion object {

        fun component(allowPlainText: Boolean = false): ComponentArgumentType =
            ComponentArgumentType(allowPlainText)

        private val INVALID_COMPONENT =
            SimpleCommandExceptionType(Component.literal("Invalid component JSON"))

        @Suppress("UNCHECKED_CAST")
        private val adapter: TypeAdapter<Component> by lazy {
            SkyHanniTypeAdapters.COMPONENT.adapter as TypeAdapter<Component>
        }
    }
}
