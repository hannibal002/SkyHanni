package at.hannibal2.skyhanni.config.commands.brigadier.arguments

import at.hannibal2.skyhanni.utils.StringUtils
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import java.util.UUID

class UuidArgumentType private constructor() : ArgumentType<UUID> {
    override fun parse(reader: StringReader): UUID {
        val input = reader.readString()
        return StringUtils.parseUUIDOrNull(input)
            ?: throw invalidUuid.createWithContext(reader)
    }

    override fun getExamples(): Collection<String> = listOf(
        "123e4567-e89b-12d3-a456-426614174000",
        "\"123e4567-e89b-12d3-a456-426614174000\"",
    )

    companion object {
        private val invalidUuid = SimpleCommandExceptionType(
            LiteralMessage("Please provide a valid UUID in the format xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"),
        )

        fun uuid(): UuidArgumentType = UuidArgumentType()
    }
}
