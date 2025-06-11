package at.hannibal2.skyhanni.config.commands.brigadier.arguments

import at.hannibal2.skyhanni.utils.LorenzVec
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType

sealed class LorenzVecArgumentType : ArgumentType<LorenzVec> {
    data object Int : LorenzVecArgumentType() {
        override fun parse(reader: StringReader): LorenzVec {
            val x = reader.readInt()
            reader.skipWhitespace()
            val y = reader.readInt()
            reader.skipWhitespace()
            val z = reader.readInt()
            return LorenzVec(x, y, z)
        }

        override fun getExamples(): Collection<String> = listOf("1 2 3", "-4 0 5")
    }
    data object Double : LorenzVecArgumentType() {
        override fun parse(reader: StringReader): LorenzVec {
            val x = reader.readDouble()
            reader.skipWhitespace()
            val y = reader.readDouble()
            reader.skipWhitespace()
            val z = reader.readDouble()
            return LorenzVec(x, y, z)
        }

        override fun getExamples(): Collection<String> = listOf("1.0 2.5 -3", "0.0 0.0 0.0")
    }

    companion object {
        /** Only accepts integers as input */
        fun int(): LorenzVecArgumentType = Int

        /** Accepts any number as input */
        fun double(): LorenzVecArgumentType = Double
    }
}
