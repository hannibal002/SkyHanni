package at.hannibal2.skyhanni.config.commands.brigadier

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType

object BrigadierArguments {

    fun integer(min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): IntegerArgumentType =
        IntegerArgumentType.integer(min, max)
    fun ArgContext.getInteger(name: String): Int {
        return IntegerArgumentType.getInteger(this, name)
    }

    fun long(min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE): LongArgumentType =
        LongArgumentType.longArg(min, max)
    fun ArgContext.getLong(name: String): Long {
        return LongArgumentType.getLong(this, name)
    }

    fun double(min: Double = Double.MIN_VALUE, max: Double = Double.MAX_VALUE): DoubleArgumentType =
        DoubleArgumentType.doubleArg(min, max)
    fun ArgContext.getDouble(name: String): Double {
        return DoubleArgumentType.getDouble(this, name)
    }

    fun float(min: Float = Float.MIN_VALUE, max: Float = Float.MAX_VALUE): FloatArgumentType =
        FloatArgumentType.floatArg(min, max)
    fun ArgContext.getFloat(name: String): Float {
        return FloatArgumentType.getFloat(this, name)
    }

    fun bool(): BoolArgumentType = BoolArgumentType.bool()
    fun ArgContext.getBool(name: String): Boolean {
        return BoolArgumentType.getBool(this, name)
    }

    fun string(): StringArgumentType = StringArgumentType.string()
    fun greedyString(): StringArgumentType = StringArgumentType.greedyString()
    fun word(): StringArgumentType = StringArgumentType.word()
    fun ArgContext.getString(name: String): String {
        return StringArgumentType.getString(this, name)
    }

    inline fun <reified T> ArgContext.getArg(name: String): T = getArgument(name, T::class.java)

}
