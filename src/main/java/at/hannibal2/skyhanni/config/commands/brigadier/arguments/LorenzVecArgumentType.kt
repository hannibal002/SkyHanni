package at.hannibal2.skyhanni.config.commands.brigadier.arguments

import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType

sealed class LorenzVecArgumentType : ArgumentType<LorenzVec> {
    data object Int : LorenzVecArgumentType() {
        override fun parse(reader: StringReader): LorenzVec {
            val playerPosition = LocationUtils.playerLocation()

            val x = if (reader.peek() == '~') {
                reader.skip()
                playerPosition.x.toInt()
            } else reader.readInt()
            reader.skipWhitespace()

            val y = if (reader.peek() == '~') {
                reader.skip()
                playerPosition.y.toInt()
            } else reader.readInt()
            reader.skipWhitespace()

            val z = if (reader.peek() == '~') {
                reader.skip()
                playerPosition.z.toInt()
            } else reader.readInt()

            return LorenzVec(x, y, z)
        }

        override fun getExamples(): Collection<String> = listOf("1 2 3", "-4 0 5", "~ 64 ~")
    }

    data object Double : LorenzVecArgumentType() {
        override fun parse(reader: StringReader): LorenzVec {
            val playerPosition = LocationUtils.playerLocation()

            val x = if (reader.peek() == '~') {
                reader.skip()
                playerPosition.x
            } else reader.readDouble()
            reader.skipWhitespace()

            val y = if (reader.peek() == '~') {
                reader.skip()
                playerPosition.y
            } else reader.readDouble()
            reader.skipWhitespace()

            val z = if (reader.peek() == '~') {
                reader.skip()
                playerPosition.z
            } else reader.readDouble()
            return LorenzVec(x, y, z)
        }

        override fun getExamples(): Collection<String> = listOf("1.0 2.5 -3", "0.0 0.0 0.0", "-1.7 ~ ~")
    }

    companion object {
        /** Only accepts integers as input */
        fun int(): LorenzVecArgumentType = Int

        /** Accepts any number as input */
        fun double(): LorenzVecArgumentType = Double
    }
}
