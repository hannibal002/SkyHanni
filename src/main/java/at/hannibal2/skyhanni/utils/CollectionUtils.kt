package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemStack
import java.util.Collections
import java.util.Queue
import java.util.WeakHashMap
import kotlin.math.ceil

object CollectionUtils {

    inline fun <reified T : Queue<E>, reified E> T.drainForEach(action: (E) -> Unit): T {
        while (true)
            action(this.poll() ?: break)
        return this
    }

    inline fun <reified T : Queue<E>, reified E> T.drain(amount: Int): T {
        for (i in 1..amount)
            this.poll() ?: break
        return this
    }

    inline fun <reified E, reified K, reified L : MutableCollection<K>>
        Queue<E>.drainTo(list: L, action: (E) -> K): L {
        while (true)
            list.add(action(this.poll() ?: break))
        return list
    }

    inline fun <reified E, reified L : MutableCollection<E>>
        Queue<E>.drainTo(list: L): L {
        while (true)
            list.add(this.poll() ?: break)
        return list
    }

    // Let garbage collector handle the removal of entries in this list
    fun <T> weakReferenceList(): MutableSet<T> = Collections.newSetFromMap(WeakHashMap<T, Boolean>())

    fun <T> MutableCollection<T>.filterToMutable(predicate: (T) -> Boolean) = filterTo(mutableListOf(), predicate)

    fun <T> List<T>.indexOfFirst(vararg args: T) = args.map { indexOf(it) }.firstOrNull { it != -1 }

    infix fun <K, V> MutableMap<K, V>.put(pairs: Pair<K, V>) {
        this[pairs.first] = pairs.second
    }

    // Taken and modified from Skytils
    @JvmStatic
    fun <T> T?.equalsOneOf(vararg other: T): Boolean {
        for (obj in other) {
            if (this == obj) return true
        }
        return false
    }

    fun <E> List<E>.getOrNull(index: Int): E? {
        return if (index in indices) {
            get(index)
        } else null
    }

    fun <T : Any> T?.toSingletonListOrEmpty(): List<T> {
        if (this == null) return emptyList()
        return listOf(this)
    }

    fun <K> MutableMap<K, Int>.addOrPut(key: K, number: Int): Int =
        this.merge(key, number, Int::plus)!! // Never returns null since "plus" can't return null

    fun <K> MutableMap<K, Long>.addOrPut(key: K, number: Long): Long =
        this.merge(key, number, Long::plus)!! // Never returns null since "plus" can't return null

    fun <K> MutableMap<K, Double>.addOrPut(key: K, number: Double): Double =
        this.merge(key, number, Double::plus)!! // Never returns null since "plus" can't return null

    fun <K> MutableMap<K, Float>.addOrPut(key: K, number: Float): Float =
        this.merge(key, number, Float::plus)!! // Never returns null since "plus" can't return null

    fun <K, N : Number> Map<K, N>.sumAllValues(): Double {
        if (values.isEmpty()) return 0.0

        return when (values.first()) {
            is Double -> values.sumOf { it.toDouble() }
            is Float -> values.sumOf { it.toDouble() }
            is Long -> values.sumOf { it.toLong() }.toDouble()
            else -> values.sumOf { it.toInt() }.toDouble()
        }
    }

    fun List<String>.nextAfter(after: String, skip: Int = 1) = nextAfter({ it == after }, skip)

    fun List<String>.nextAfter(after: (String) -> Boolean, skip: Int = 1): String? {
        var missing = -1
        for (line in this) {
            if (after(line)) {
                missing = skip - 1
                continue
            }
            if (missing == 0) {
                return line
            }
            if (missing != -1) {
                missing--
            }
        }
        return null
    }

    fun List<String>.removeNextAfter(after: String, skip: Int = 1) = removeNextAfter({ it == after }, skip)

    fun List<String>.removeNextAfter(after: (String) -> Boolean, skip: Int = 1): List<String> {
        val newList = mutableListOf<String>()
        var missing = -1
        for (line in this) {
            if (after(line)) {
                missing = skip - 1
                continue
            }
            if (missing == 0) {
                missing--
                continue
            }
            if (missing != -1) {
                missing--
            }
            newList.add(line)
        }
        return newList
    }

    fun List<String>.addIfNotNull(element: String?) = element?.let { plus(it) } ?: this

    fun <K, V> Map<K, V>.editCopy(function: MutableMap<K, V>.() -> Unit) =
        toMutableMap().also { function(it) }.toMap()

    fun <T> List<T>.editCopy(function: MutableList<T>.() -> Unit) =
        toMutableList().also { function(it) }.toList()

    fun <K, V> Map<K, V>.moveEntryToTop(matcher: (Map.Entry<K, V>) -> Boolean): Map<K, V> {
        val entry = entries.find(matcher)
        if (entry != null) {
            val newMap = linkedMapOf(entry.key to entry.value)
            newMap.putAll(this)
            return newMap
        }
        return this
    }

    operator fun IntRange.contains(range: IntRange): Boolean =
        range.first in this && range.last in this

    fun <E> MutableList<List<E>>.addAsSingletonList(text: E) {
        add(Collections.singletonList(text))
    }

    fun MutableList<List<Renderable>>.addSingleString(text: String) {
        add(Collections.singletonList(Renderable.string(text)))
    }

    fun <K, V : Comparable<V>> List<Pair<K, V>>.sorted(): List<Pair<K, V>> {
        return sortedBy { (_, value) -> value }
    }

    fun <K, V : Comparable<V>> Map<K, V>.sorted(): Map<K, V> {
        return toList().sorted().toMap()
    }

    fun <K, V : Comparable<V>> Map<K, V>.sortedDesc(): Map<K, V> {
        return toList().sorted().reversed().toMap()
    }

    fun <T> Sequence<T>.takeWhileInclusive(predicate: (T) -> Boolean) = sequence {
        with(iterator()) {
            while (hasNext()) {
                val next = next()
                yield(next)
                if (!predicate(next)) break
            }
        }
    }

    inline fun <T, R> Iterator<T>.consumeWhile(block: (T) -> R): R? {
        while (hasNext()) {
            return block(next()) ?: continue
        }
        return null
    }

    inline fun <T> Iterator<T>.collectWhile(block: (T) -> Boolean): List<T> {
        return collectWhileTo(mutableListOf(), block)
    }

    inline fun <T, C : MutableCollection<T>> Iterator<T>.collectWhileTo(collection: C, block: (T) -> Boolean): C {
        while (hasNext()) {
            val element = next()
            if (block(element)) {
                collection.add(element)
            } else {
                break
            }
        }
        return collection
    }


    /** Updates a value if it is present in the set (equals), useful if the newValue is not reference equal with the value in the set */
    inline fun <reified T> MutableSet<T>.refreshReference(newValue: T) = if (this.contains(newValue)) {
        this.remove(newValue)
        this.add(newValue)
        true
    } else false

    @Suppress("UNCHECKED_CAST")
    fun <T> Iterable<T?>.takeIfAllNotNull(): Iterable<T>? =
        takeIf { null !in this } as? Iterable<T>

    @Suppress("UNCHECKED_CAST")
    fun <T> List<T?>.takeIfAllNotNull(): List<T>? =
        takeIf { null !in this } as? List<T>

    // TODO add cache
    fun MutableList<Renderable>.addString(
        text: String,
        horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
        verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
    ) {
        add(Renderable.string(text, horizontalAlign = horizontalAlign, verticalAlign = verticalAlign))
    }

    // TODO add internal name support, and caching
    fun MutableList<Renderable>.addItemStack(
        itemStack: ItemStack,
        highlight: Boolean = false,
        scale: Double = NEUItems.itemFontSize,
    ) {
        if (highlight) {
            // Hack to add enchant glint, like Hypixel does it
            itemStack.addEnchantment(Enchantment.protection, 0)
        }
        add(Renderable.itemStack(itemStack, scale = scale))
    }

    fun MutableList<Renderable>.addItemStack(internalName: NEUInternalName) {
        addItemStack(internalName.getItemStack())
    }

    inline fun <reified T : Enum<T>> MutableList<Renderable>.addSelector(
        prefix: String,
        getName: (T) -> String,
        isCurrent: (T) -> Boolean,
        crossinline onChange: (T) -> Unit,
    ) {
        add(Renderable.horizontalContainer(buildSelector<T>(prefix, getName, isCurrent, onChange)))
    }

    inline fun <reified T : Enum<T>> buildSelector(
        prefix: String,
        getName: (T) -> String,
        isCurrent: (T) -> Boolean,
        crossinline onChange: (T) -> Unit,
    ) = buildList {
        addString(prefix)
        for (entry in enumValues<T>()) {
            val display = getName(entry)
            if (isCurrent(entry)) {
                addString("§a[$display]")
            } else {
                addString("§e[")
                add(Renderable.link("§e$display") {
                    onChange(entry)
                })
                addString("§e]")
            }
            addString(" ")
        }
    }

    inline fun MutableList<Renderable>.addButton(
        prefix: String,
        getName: String,
        crossinline onChange: () -> Unit,
        tips: List<String> = emptyList(),
    ) {
        val onClick = {
            if ((System.currentTimeMillis() - ChatUtils.lastButtonClicked) > 150) { // funny thing happen if I don't do that
                onChange()
                SoundUtils.playClickSound()
                ChatUtils.lastButtonClicked = System.currentTimeMillis()
            }
        }
        add(Renderable.horizontalContainer(buildList {
            addString(prefix)
            addString("§a[")
            if (tips.isEmpty()) {
                add(Renderable.link("§e$getName", false, onClick))
            } else {
                add(Renderable.clickAndHover("§e$getName", tips, false, onClick))
            }
            addString("§a]")
        }))
    }

    fun Collection<Collection<Renderable>>.tableStretchXPadding(xSpace: Int): Int {
        if (this.isEmpty()) return xSpace
        val off = RenderableUtils.calculateTableXOffsets(this as List<List<Renderable?>>, 0)
        val xLength = off.size - 1
        val emptySpace = xSpace - off.last()
        if (emptySpace < 0) {
            //    throw IllegalArgumentException("Not enough space for content")
        }
        return emptySpace / (xLength - 1)
    }

    fun Collection<Collection<Renderable>>.tableStretchYPadding(ySpace: Int): Int {
        if (this.isEmpty()) return ySpace
        val off = RenderableUtils.calculateTableYOffsets(this as List<List<Renderable?>>, 0)
        val yLength = off.size - 1
        val emptySpace = ySpace - off.last()
        if (emptySpace < 0) {
            //    throw IllegalArgumentException("Not enough space for content")
        }
        return emptySpace / (yLength - 1)
    }

    /** Splits the input into equal sized lists. If the list can't get divided clean by [subs] then the last entry gets reduced. e.g. 13/4 = [4,4,4,1]*/
    fun <T> Collection<T>.split(subs: Int = 2): List<List<T>> {
        if (this.isEmpty()) return listOf(emptyList())
        val list = this.chunked(ceil(this.size.toDouble() / subs.toDouble()).toInt()).toMutableList()
        while (list.size < subs) {
            list.add(emptyList())
        }
        return list
    }

    inline fun <K, V, R : Any> Map<K, V>.mapKeysNotNull(transform: (Map.Entry<K, V>) -> R?): Map<R, V> {
        val destination = LinkedHashMap<R, V>()
        for (element in this) {
            val newKey = transform(element)
            if (newKey != null) {
                destination[newKey] = element.value
            }
        }
        return destination
    }

    inline fun <T, C : Number, D : Number> Iterable<T>.sumOfPair(selector: (T) -> Pair<C, D>): Pair<Double, Double> {
        var sum = Pair(0.0, 0.0)
        for (element in this) {
            val add = selector(element)
            sum = sum.first + add.first.toDouble() to sum.second + add.second.toDouble()
        }
        return sum
    }

    inline fun <T, R> Iterable<T>.zipWithNext3(transform: (a: T, b: T, c: T) -> R): List<R> {
        val iterator = iterator()
        if (!iterator.hasNext()) return emptyList()
        var one = iterator.next()
        if (!iterator.hasNext()) return emptyList()
        var two = iterator.next()
        val result = mutableListOf<R>()
        while (iterator.hasNext()) {
            val next = iterator.next()
            result.add(transform(one, two, next))
            one = two
            two = next
        }
        return result
    }

    fun <T> Iterable<T>.zipWithNext3(): List<Triple<T, T, T>> {
        return zipWithNext3 { a, b, c -> Triple(a, b, c) }
    }

    fun <K, V : Any> Map<K?, V>.filterNotNullKeys(): Map<K, V> {
        return filterKeys { it != null } as Map<K, V>
    }
}
