package at.hannibal2.skyhanni.config.commands.brigadier.arguments

import at.hannibal2.skyhanni.utils.StringUtils
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.LiteralMessage
import java.util.UUID

class UuidArgumentType : ArgumentType<UUID> {
    override fun parse(reader: StringReader): UUID {
        val input =
            if (reader.canRead() && reader.peek() == '"')
                reader.readQuotedString()
            else
                readUnquoted(reader)

        return StringUtils.parseUUIDOrNull(input)
            ?: throw invalidUuid.createWithContext(reader)
    }

    private fun readUnquoted(reader: StringReader): String {
        val start = reader.cursor
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip()
        }
        return reader.string.substring(start, reader.cursor)
    }

    override fun getExamples(): Collection<String> = listOf(
        "123e4567-e89b-12d3-a456-426614174000",
        "\"123e4567-e89b-12d3-a456-426614174000\""
    )

    companion object {
        private val invalidUuid = SimpleCommandExceptionType(
            LiteralMessage(
                "Invalid UUID format\n" +
                    "Please provide a valid UUID in the format xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
            )
        )

        fun uuid(): UuidArgumentType = UuidArgumentType()
    }
}
