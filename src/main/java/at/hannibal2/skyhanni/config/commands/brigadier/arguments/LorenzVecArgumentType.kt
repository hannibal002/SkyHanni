package at.hannibal2.skyhanni.config.commands.brigadier.arguments

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType

sealed class LorenzVecArgumentType : ArgumentType<LorenzVec> {

    protected abstract fun toVec(x: kotlin.Double, y: kotlin.Double, z: kotlin.Double): LorenzVec

    override fun parse(reader: StringReader): LorenzVec {
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

    private fun parseCoords(input: String): LorenzVec {
        val playerPos = LocationUtils.playerLocation()
        for (pattern in patterns) {
            pattern.matchMatcher(input) {
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

        override fun getExamples(): Collection<String> = listOf("1 2 3", "-4 0 5", "~ 64 ~", "1:2:3", "LorenzVec(1, 2, 3)")
    }

    data object Double : LorenzVecArgumentType() {
        override fun toVec(x: kotlin.Double, y: kotlin.Double, z: kotlin.Double) = LorenzVec(x, y, z)

        override fun getExamples(): Collection<String> =
            listOf("1.0 2.5 -3", "0.0 0.0 0.0", "-1.7 ~ ~", "-78.8:68.0:-28.7", "LorenzVec(-91.7, 70.0, 29.3)")
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
            """LorenzVec\((?<x>-?\d+(?:\.\d+)?),\s*(?<y>-?\d+(?:\.\d+)?),\s*(?<z>-?\d+(?:\.\d+)?)\)""",
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

        private val patterns = listOf(lorenzVecPattern, colonPattern, spacePattern)

        private val invalidCoordinates = SimpleCommandExceptionType(LiteralMessage("Invalid coordinates"))

        /** Only accepts integers as input */
        fun int(): LorenzVecArgumentType = Int

        /** Accepts any number as input */
        fun double(): LorenzVecArgumentType = Double
    }
}
