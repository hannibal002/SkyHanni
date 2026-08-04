package at.hannibal2.skyhanni.config.commands.brigadier.arguments

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import java.awt.Color

sealed class LorenzVecArgumentType : ArgumentType<LorenzVec> {

    protected abstract fun toVec(x: kotlin.Double, y: kotlin.Double, z: kotlin.Double): LorenzVec

    override fun parse(reader: StringReader): LorenzVec {
        val input = if (reader.canRead() && reader.peek() == '"') reader.readQuotedString()
        else consumeMatch(reader)
        return parseCoords(input)
    }

    /**
     * Everything left of the coordinates is ignored, so that chat lines can be pasted directly.
     * Skipping to the end of the match keeps the reader positioned behind the coordinates.
     */
    private fun consumeMatch(reader: StringReader): String {
        val remaining = reader.remaining
        for (pattern in patterns) {
            val (coords, end) = pattern.findMatcher(remaining) { group() to end() } ?: continue
            repeat(end) { reader.skip() }
            return coords
        }
        throw invalidCoordinates.createWithContext(reader)
    }

    private fun parseCoords(input: String): LorenzVec {
        val playerPos = LocationUtils.playerLocation()
        for (pattern in patterns) {
            pattern.findMatcher(input) {
                val x = if (group("x") == "~") playerPos.x else group("x").toDouble()
                val y = if (group("y") == "~") playerPos.y else group("y").toDouble()
                val z = if (group("z") == "~") playerPos.z else group("z").toDouble()
                return toVec(x, y, z)
            }
        }
        throw invalidCoordinates.create()
    }

    data object Int : LorenzVecArgumentType() {
        override fun toVec(x: kotlin.Double, y: kotlin.Double, z: kotlin.Double) =
            LorenzVec(x.toInt(), y.toInt(), z.toInt())

        override fun getExamples(): Collection<String> =
            listOf("1 2 3", "-4 0 5", "~ 64 ~", "1:2:3", "-4, 0, 5", "LorenzVec(1, 2, 3)", "x: -262, y: 58, z: 117")
    }

    data object Double : LorenzVecArgumentType() {
        override fun toVec(x: kotlin.Double, y: kotlin.Double, z: kotlin.Double) = LorenzVec(x, y, z)

        override fun getExamples(): Collection<String> = listOf(
            "1.0 2.5 -3",
            "0.0 0.0 0.0",
            "-1.7 ~ ~",
            "-78.8:68.0:-28.7",
            "-1.7, 2.5, -3.0",
            "LorenzVec(-91.7, 70.0, 29.3)",
            "x: -262.0, y: 58.0, z: 117.0",
        )
    }

    @SkyHanniModule
    companion object {

        private val patternGroup = RepoPattern.group("commands.brigadier.arguments.lorenzvec")

        /**
         * REGEX-TEST: LorenzVec(-91.7, 70.0, 29.3)
         * REGEX-TEST: LorenzVec(1, 2, 3)
         * REGEX-TEST: LorenzVec(0.0, 0.0, 0.0)
         * REGEX-TEST: LorenzVec(-78.8, 68.0, -28.7)
         */
        private val lorenzVecPattern by patternGroup.pattern(
            "lorenz",
            "LorenzVec\\((?<x>-?\\d+(?:\\.\\d+)?),\\s*(?<y>-?\\d+(?:\\.\\d+)?),\\s*(?<z>-?\\d+(?:\\.\\d+)?)\\)",
        )

        /**
         * REGEX-TEST: x=-262.0, y=58.0, z=117.0
         * REGEX-TEST: x: -598, y: 138, z: 235
         * REGEX-TEST: x:-598,y:138,z:235
         * REGEX-TEST: x=-49, y=79, z=-151
         */
        private val namedParameterPattern by patternGroup.pattern(
            "named-parameter",
            "x[=:]\\s*(?<x>-?\\d+(?:\\.\\d+)?),\\s*y[=:]\\s*(?<y>-?\\d+(?:\\.\\d+)?),\\s*z[=:]\\s*(?<z>-?\\d+(?:\\.\\d+)?)",
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
            "(?<x>~|-?\\d+(?:\\.\\d+)?)\\s+(?<y>~|-?\\d+(?:\\.\\d+)?)\\s+(?<z>~|-?\\d+(?:\\.\\d+)?)",
        )

        /**
         * REGEX-TEST: -49, 79, -151
         * REGEX-TEST: -49,79,-151
         * REGEX-TEST: 1.5, 2, -3.25
         */
        private val commaPattern by patternGroup.pattern(
            "comma",
            "(?<x>-?\\d+(?:\\.\\d+)?),\\s*(?<y>-?\\d+(?:\\.\\d+)?),\\s*(?<z>-?\\d+(?:\\.\\d+)?)",
        )

        /**
         * Last in [patterns], because a bare colon triple is indistinguishable from a chat timestamp.
         *
         * REGEX-TEST: -78.8:68.0:-28.7
         * REGEX-TEST: 1:2:3
         * REGEX-TEST: ~:64:~
         * REGEX-TEST: ~:~:~
         */
        private val colonPattern by patternGroup.pattern(
            "colon",
            "(?<x>~|-?\\d+(?:\\.\\d+)?):(?<y>~|-?\\d+(?:\\.\\d+)?):(?<z>~|-?\\d+(?:\\.\\d+)?)",
        )

        /**
         * The order matters. [consumeMatch] takes the first pattern that matches anywhere in the input, not the
         * match closest to the start. Patterns that identify themselves through a keyword or a separator come
         * first, ambiguous ones last, so that unrelated text next to the coordinates cannot win.
         */
        private val patterns = listOf(lorenzVecPattern, namedParameterPattern, spacePattern, commaPattern, colonPattern)

        private val invalidCoordinates = SimpleCommandExceptionType(LiteralMessage("Invalid coordinates"))

        /** Only accepts integers as input */
        fun int(): LorenzVecArgumentType = Int

        /** Accepts any number as input */
        fun double(): LorenzVecArgumentType = Double
    }
}
