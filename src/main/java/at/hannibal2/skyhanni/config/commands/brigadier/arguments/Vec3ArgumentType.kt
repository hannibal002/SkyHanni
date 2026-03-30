package at.hannibal2.skyhanni.config.commands.brigadier.arguments

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.world.phys.Vec3

/**
 * Fabric-compatible alternative to [Vec3Argument] that does not depend on server state.
 */
sealed class Vec3ArgumentType : ArgumentType<Vec3> {

    override fun parse(reader: StringReader): Vec3 {
        val input = if (reader.canRead() && reader.peek() == '"') reader.readQuotedString()
        else consumeMatch(reader)

        return parseCoords(input)
    }

    private fun consumeMatch(reader: StringReader): String {
        val remaining = reader.remaining
        for (pattern in patterns) {
            val matched = pattern.findMatcher(remaining) {
                if (start() == 0) group() else null
            } ?: continue
            repeat(matched.length) { reader.skip() }
            return matched
        }
        throw invalidCoordinates.createWithContext(reader)
    }

    private fun parseCoords(input: String): Vec3 {
        val playerPos = LocationUtils.playerLocation()
        for (pattern in patterns) {
            pattern.matchMatcher(input) {
                val x = if (group("x") == "~") playerPos.x else group("x").toDouble()
                val y = if (group("y") == "~") playerPos.y else group("y").toDouble()
                val z = if (group("z") == "~") playerPos.z else group("z").toDouble()
                return Vec3(x, y, z)
            }
        }
        throw invalidCoordinates.create()
    }


    data object Int : Vec3ArgumentType() {
        override fun getExamples(): Collection<String> =
            listOf("1 2 3", "-4 0 5", "~ 64 ~", "1:2:3", "Vec3(1, 2, 3)")
    }

    data object Double : Vec3ArgumentType() {
        override fun getExamples(): Collection<String> =
            listOf("1.0 2.5 -3", "0.0 0.0 0.0", "-1.7 ~ ~", "-78.8:68.0:-28.7", "Vec3(-91.7, 70.0, 29.3)")
    }

    @SkyHanniModule
    companion object {

        private val patternGroup = RepoPattern.group("commands.brigadier.arguments.vec3")

        /**
         * REGEX-TEST: Vec3(-91.7, 70.0, 29.3)
         * REGEX-TEST: Vec3(1, 2, 3)
         * REGEX-TEST: Vec3(0.0, 0.0, 0.0)
         * REGEX-TEST: Vec3(-78.8, 68.0, -28.7)
         */
        private val vec3Pattern by patternGroup.pattern(
            "vec3",
            """Vec3\((?<x>-?\d+(?:\.\d+)?),\s*(?<y>-?\d+(?:\.\d+)?),\s*(?<z>-?\d+(?:\.\d+)?)\)""",
        )

        /**
         * REGEX-TEST: -78.8:68.0:-28.7
         * REGEX-TEST: 1:2:3
         * REGEX-TEST: ~:64:~
         * REGEX-TEST: ~:~:~
         */
        private val colonPattern by patternGroup.pattern(
            "colon",
            """(?<x>~|-?\d+(?:\.\d+)?):(?<y>~|-?\d+(?:\.\d+)?):(?<z>~|-?\d+(?:\.\d+)?)""",
        )

        /**
         * REGEX-TEST: 1 2 3
         * REGEX-TEST: -4 0 5
         * REGEX-TEST: ~ 64 ~
         * REGEX-TEST: 1.0 2.5 -3
         * REGEX-TEST: -1.7 ~ ~
         * REGEX-TEST: 0.0 0.0 0.0
         */
        private val spacePattern by patternGroup.pattern(
            "space",
            """(?<x>~|-?\d+(?:\.\d+)?)\s+(?<y>~|-?\d+(?:\.\d+)?)\s+(?<z>~|-?\d+(?:\.\d+)?)""",
        )

        private val patterns = listOf(vec3Pattern, colonPattern, spacePattern)

        private val invalidCoordinates =
            SimpleCommandExceptionType(LiteralMessage("Invalid coordinates"))

        /** Only accepts integers as input */
        fun int(): Vec3ArgumentType = Int

        /** Accepts any number as input */
        fun double(): Vec3ArgumentType = Double
    }
}
