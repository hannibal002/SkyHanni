package at.hannibal2.skyhanni.utils.compat

import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.FloatTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.LongTag
import net.minecraft.nbt.StringTag
import kotlin.jvm.optionals.getOrNull

object NbtCompat {

    // stolen from some nbt constants file
    const val TAG_END: Int = 0
    const val TAG_BYTE: Int = 1
    const val TAG_SHORT: Int = 2
    const val TAG_INT: Int = 3
    const val TAG_LONG: Int = 4
    const val TAG_FLOAT: Int = 5
    const val TAG_DOUBLE: Int = 6
    const val TAG_BYTE_ARRAY: Int = 7
    const val TAG_STRING: Int = 8
    const val TAG_LIST: Int = 9
    const val TAG_COMPOUND: Int = 10
    const val TAG_INT_ARRAY: Int = 11
    const val TAG_ANY_NUMERIC = 99

    fun containsList(list: CompoundTag, key: String): Boolean {
        return list.contains(key) && list.get(key) is ListTag
    }

    fun containsCompound(compound: CompoundTag, key: String): Boolean {
        return compound.contains(key) && compound.get(key) is CompoundTag
    }

    fun getStringTagList(list: CompoundTag, key: String): ListTag {
        val nbtList = list.getList(key).getOrNull() ?: ListTag()
        return getList(nbtList, TAG_STRING)
    }

    fun getCompoundTagList(list: CompoundTag, key: String): ListTag {
        val nbtList = list.getList(key).getOrNull() ?: ListTag()
        return getList(nbtList, TAG_COMPOUND)
    }

    private fun getList(list: ListTag, type: Int): ListTag {
        for (nbtElement in list) {
            when (type) {
                TAG_STRING -> if (nbtElement !is StringTag) return ListTag()
                TAG_COMPOUND -> if (nbtElement !is CompoundTag) return ListTag()
                TAG_INT -> if (nbtElement !is IntTag) return ListTag()
                TAG_DOUBLE -> if (nbtElement !is DoubleTag) return ListTag()
                TAG_LONG -> if (nbtElement !is LongTag) return ListTag()
                TAG_BYTE -> if (nbtElement !is ByteTag) return ListTag()
                TAG_FLOAT -> if (nbtElement !is FloatTag) return ListTag()
            }
        }
        return list
    }
}

fun CompoundTag.getStringOrDefault(key: String): String {
    return this.getString(key).getOrNull().orEmpty()
}

fun CompoundTag.getIntOrDefault(key: String?): Int {
    return this.getInt(key).getOrNull() ?: 0
}

fun CompoundTag.getLongOrDefault(key: String): Long {
    return this.getLong(key).getOrNull() ?: 0
}

fun CompoundTag.getFloatOrDefault(key: String): Float {
    return this.getFloat(key).getOrNull() ?: 0f
}

fun CompoundTag.getDoubleOrDefault(key: String): Double {
    return this.getDouble(key).getOrNull() ?: 0.0
}

fun CompoundTag.getBooleanOrDefault(key: String): Boolean {
    return this.getBoolean(key).getOrNull() ?: false
}

fun CompoundTag.getByteOrDefault(key: String): Byte? {
    return this.getByte(key).getOrNull()
}

fun CompoundTag.getCompoundOrDefault(key: String): CompoundTag {
    if (this.getCompound(key).isEmpty) {
        return CompoundTag()
    }
    return this.getCompound(key).get()
}

fun ListTag.getStringOrDefault(index: Int): String {
    return this.getString(index).getOrNull().orEmpty()
}

fun ListTag.getCompoundOrDefault(index: Int): CompoundTag {
    return this.getCompound(index).getOrNull() ?: CompoundTag()
}
