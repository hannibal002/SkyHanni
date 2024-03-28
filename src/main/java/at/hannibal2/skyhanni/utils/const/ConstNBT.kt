package at.hannibal2.skyhanni.utils.const

import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagInt
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagLong
import net.minecraft.nbt.NBTTagString


/**
 * This method indexes into an NBTTagCompound searching for a tag.
 * The caller must guarantee that the [T]s type identifier is [tag].
 * This means for any `instance` of [NBTBase], `instance.getId() == tag` iff `instance is T`.
 */
@PublishedApi
internal fun <T : NBTBase> Const<NBTTagCompound>.getAndCast(name: String, tag: Int): Const<T>? =
    unsafeMap {
        if (it.hasKey(name, tag))
            @Suppress("UNCHECKED_CAST")
            it.getTag(name) as T
        else
            null
    }.liftNull()

private val TAG_COMPOUND_KEY = NBTTagCompound().id.toInt()
private val TAG_COMPOUND_STRING = NBTTagString().id.toInt()
private val TAG_COMPOUND_INT = NBTTagInt(0).id.toInt()
private val TAG_COMPOUND_LONG = NBTTagLong(0L).id.toInt()
private val TAG_COMPOUND_LIST = NBTTagList().id.toInt()

fun Const<NBTTagCompound>.getTagCompound(name: String): Const<NBTTagCompound>? =
    getAndCast(name, TAG_COMPOUND_KEY)

fun Const<NBTTagCompound>.getString(name: String): Const<NBTTagString>? =
    getAndCast(name, TAG_COMPOUND_STRING)

fun Const<NBTTagCompound>.getInt(name: String): Const<NBTTagInt>? =
    getAndCast(name, TAG_COMPOUND_INT)

fun Const<NBTTagCompound>.getLong(name: String): Const<NBTTagLong>? =
    getAndCast(name, TAG_COMPOUND_LONG)

fun Const<NBTTagCompound>.getList(name: String): Const<NBTTagList>? =
    getAndCast(name, TAG_COMPOUND_LIST)

val Const<NBTTagList>.size: Int
    get() = unsafeMap(NBTTagList::tagCount).unconst

fun Const<NBTTagList>.getTag(i: Int): Const<NBTBase> {
    return unsafeMap { it.get(i) }
}

inline fun Const<NBTTagList>.intoList(): Const<List<NBTBase>> {
    return unsafeMap {
        val build = mutableListOf<NBTBase>()
        for (i in 0..<it.tagCount()) {
            build.add(it.get(i))
        }
        build
    }
}

fun Const<NBTTagString>.getString(): String = unsafeMap(NBTTagString::getString).unconst
fun Const<NBTTagInt>.getInt(): Int = unsafeMap(NBTTagInt::getInt).unconst
fun Const<NBTTagLong>.getLong(): Long = unsafeMap(NBTTagLong::getLong).unconst

