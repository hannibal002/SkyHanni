package at.hannibal2.skyhanni.utils.compat

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList

//#if MC > 1.21
//$$ import kotlin.jvm.optionals.getOrNull
//$$ import net.minecraft.nbt.NbtString
//$$ import net.minecraft.nbt.NbtByte
//$$ import net.minecraft.nbt.NbtDouble
//$$ import net.minecraft.nbt.NbtFloat
//$$ import net.minecraft.nbt.NbtInt
//$$ import net.minecraft.nbt.NbtLong
//#endif

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

    fun containsList(list: NBTTagCompound, key: String): Boolean {
        //#if MC < 1.21
        return list.hasKey(key, TAG_LIST)
        //#else
        //$$ return list.contains(key) && list.get(key) is NbtList
        //#endif
    }

    fun containsCompound(compound: NBTTagCompound, key: String): Boolean {
        //#if MC < 1.21
        return compound.hasKey(key, TAG_COMPOUND)
        //#else
        //$$ return compound.contains(key) && compound.get(key) is NbtCompound
        //#endif
    }

    fun getStringTagList(list: NBTTagCompound, key: String): NBTTagList {
        //#if MC < 1.21
        return list.getTagList(key, TAG_STRING)
        //#else
        //$$ val nbtList = list.getList(key).getOrNull() ?: NbtList()
        //$$ return getList(nbtList, TAG_STRING)
        //#endif
    }

    fun getCompoundTagList(list: NBTTagCompound, key: String): NBTTagList {
        //#if MC < 1.21
        return list.getTagList(key, TAG_COMPOUND)
        //#else
        //$$ val nbtList = list.getList(key).getOrNull() ?: NbtList()
        //$$ return getList(nbtList, TAG_COMPOUND)
        //#endif
    }

    //#if MC > 1.21
    //$$ private fun getList(list: NbtList, type: Int): NbtList {
    //$$     for (nbtElement in list) {
    //$$         when (type) {
    //$$             TAG_STRING -> if (nbtElement !is NbtString) return NbtList()
    //$$             TAG_COMPOUND -> if (nbtElement !is NbtCompound) return NbtList()
    //$$             TAG_INT -> if (nbtElement !is NbtInt) return NbtList()
    //$$             TAG_DOUBLE -> if (nbtElement !is NbtDouble) return NbtList()
    //$$             TAG_LONG -> if (nbtElement !is NbtLong) return NbtList()
    //$$             TAG_BYTE -> if (nbtElement !is NbtByte) return NbtList()
    //$$             TAG_FLOAT -> if (nbtElement !is NbtFloat) return NbtList()
    //$$         }
    //$$     }
    //$$     return list
    //$$ }
    //#endif
}

//#if MC > 1.21
//$$
//$$ fun NbtCompound.getStringOrDefault(key: String): String {
//$$     return this.getString(key).getOrNull() ?: ""
//$$ }
//$$
//$$ fun NbtCompound.getIntOrDefault(key: String?): Int {
//$$     return this.getInt(key).getOrNull() ?: 0
//$$ }
//$$
//$$ fun NbtCompound.getLongOrDefault(key: String): Long {
//$$     return this.getLong(key).getOrNull()  ?: 0
//$$ }
//$$
//$$ fun NbtCompound.getFloatOrDefault(key: String): Float {
//$$     return this.getFloat(key).getOrNull() ?: 0f
//$$ }
//$$
//$$ fun NbtCompound.getDoubleOrDefault(key: String): Double {
//$$     return this.getDouble(key).getOrNull() ?: 0.0
//$$ }
//$$
//$$ fun NbtCompound.getBooleanOrDefault(key: String): Boolean {
//$$     return this.getBoolean(key).getOrNull()  ?: false
//$$ }
//$$
//$$ fun NbtCompound.getByteOrDefault(key: String): Byte? {
//$$     return this.getByte(key).getOrNull()
//$$ }
//$$
//$$ fun NbtCompound.getCompoundOrDefault(key: String): NbtCompound {
//$$     if (this.getCompound(key).isEmpty) {
//$$         return NbtCompound()
//$$     }
//$$     return this.getCompound(key).get()
//$$ }
//$$
//$$ fun NbtList.getStringOrDefault(index: Int): String {
//$$     return this.getString(index).getOrNull() ?: ""
//$$ }
//$$
//$$ fun NbtList.getCompoundOrDefault(index: Int): NbtCompound {
//$$     return this.getCompound(index).getOrNull() ?: NbtCompound()
//$$ }
//#endif
