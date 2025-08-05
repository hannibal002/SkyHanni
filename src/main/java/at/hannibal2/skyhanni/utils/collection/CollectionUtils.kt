package at.hannibal2.skyhanni.utils.collection

import at.hannibal2.skyhanni.utils.MinMaxNumber
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Collections
import java.util.EnumMap
import java.util.PriorityQueue
import java.util.Queue
import java.util.WeakHashMap
import java.util.regex.Pattern
import kotlin.math.ceil
import kotlin.time.Duration

@Suppress("TooManyFunctions")
object CollectionUtils {

    inline fun <reified T : Queue<E>, reified E> T.drainForEach(action: (E) -> Unit): T {
        while (true) action(this.poll() ?: break)
        return this
    }

    inline fun <reified E, reified L : MutableCollection<E>> Queue<E>.drainTo(list: L): L {
        while (true) list.add(this.poll() ?: break)
        return list
    }

    // Let garbage collector handle the removal of entries in this list
    fun <T> weakReferenceList(): MutableSet<T> = Collections.newSetFromMap(WeakHashMap<T, Boolean>())

    fun <T> MutableList<T>.filterToMutable(predicate: (T) -> Boolean) = filterTo(mutableListOf(), predicate)

    fun <T> List<T>.indexOfFirst(vararg args: T): Int? {
        if (args.isEmpty()) return null
        val set = args.toSet()
        forEachIndexed { index, item ->
            if (item in set) return index
        }
        return null
    }

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

    fun <T : Any> T?.toSingletonListOrEmpty(): List<T> = listOfNotNull(this)

    fun <K> MutableMap<K, Int>.addOrPut(key: K, number: Int): Int =
        this.merge(key, number, Int::plus)!! // Never returns null since "plus" can't return null

    fun <K> MutableMap<K, Long>.addOrPut(key: K, number: Long): Long =
        this.merge(key, number, Long::plus)!! // Never returns null since "plus" can't return null

    fun <K> MutableMap<K, Double>.addOrPut(key: K, number: Double): Double =
        this.merge(key, number, Double::plus)!! // Never returns null since "plus" can't return null

    fun <K> MutableMap<K, Float>.addOrPut(key: K, number: Float): Float =
        this.merge(key, number, Float::plus)!! // Never returns null since "plus" can't return null

    @Suppress("UnsafeCallOnNullableType")
    fun <K> MutableMap<K, Duration>.addOrPut(key: K, number: Duration): Duration =
        this.merge(key, number, Duration::plus)!! // Never returns null since "plus" can't return null

    @Suppress("UnsafeCallOnNullableType")
    fun <K> MutableMap<K, MinMaxNumber>.addOrPut(key: K, number: MinMaxNumber): MinMaxNumber =
        this.merge(key, number, MinMaxNumber::plus)!! // Never returns null since "plus" can't return null

    fun <K, N : Number> Map<K, N>.sumAllValues(): Double {
        if (values.isEmpty()) return 0.0

        return when (values.first()) {
            is Double -> values.sumOf { it.toDouble() }
            is Float -> values.sumOf { it.toDouble() }
            is Long -> values.sumOf { it.toLong() }.toDouble()
            else -> values.sumOf { it.toInt() }.toDouble()
        }
    }

    /**
     * Subtracts the values of the [other] map FROM the values of [this] map, for each key that exists in both maps.
     * If a key exists in [this] map but not in [other], its value remains unchanged.
     * If a key exists in [other] but not in [this], the result will reflect a -1 * of [other]'s value.
     */
    fun <K, V : Number> Map<K, V>.subtract(other: Map<K, V>): Map<K, Double> {
        val combKeys = (this.keys + other.keys).toSet()
        return combKeys.associateWith {
            val thisValue = (this[it] ?: 0).toDouble()
            val otherValue = (other[it] ?: 0).toDouble()
            val diff = thisValue - otherValue
            diff
        }
    }

    /**
     * Same deal as [subtract], but allows you to transform the result of the subtraction into a different type.
     */
    inline fun <K, V : Number, R> Map<K, V>.subtract(
        other: Map<K, V>,
        transform: (Double) -> R
    ): Map<K, R> = (keys + other.keys).associateWith { k ->
        val diff = (this[k]?.toDouble() ?: 0.0) - (other[k]?.toDouble() ?: 0.0)
        transform(diff)
    }

    fun <K, V : Number> List<Map<K, V>>.sumByKey(): Map<K, Double> =
        flatMap { it.entries }.groupBy({ it.key }, { it.value.toDouble() }).mapValues { (_, values) -> values.sum() }

    fun <T, R> Sequence<IndexedValue<T>>.runningIndexedFold(initial: R, operation: (R, T) -> R): Sequence<IndexedValue<R>> =
        map { it.value }.runningFold(initial, operation).zip(map { it.index }) { value, index -> IndexedValue(index, value) }

    suspend inline fun <T, R> Iterable<T>.mapAsync(
        crossinline transform: (T) -> R
    ): List<R> = coroutineScope {
        map {
            async { transform(it) }
        }.awaitAll()
    }

    suspend inline fun <T, R> Iterable<T>.mapNotNullAsync(
        crossinline transform: (T) -> R?
    ): List<R> = coroutineScope {
        mapNotNull {
            async { transform(it) }
        }.awaitAll().filterNotNull()
    }

    fun <T : Any> Sequence<T>.firstTwiceOf(a: (T) -> Boolean, b: (T) -> Boolean): Pair<T?, T?> {
        var firstA: T? = null
        var firstB: T? = null

        for (item in this) {
            if (firstA == null && a(item)) firstA = item
            if (firstB == null && b(item)) firstB = item
            if (firstA != null && firstB != null) break
        }
        return Pair(firstA, firstB)
    }

    /** Returns a map containing the count of occurrences of each distinct result of the [selector] function. */
    inline fun <T, K> Iterable<T>.countBy(selector: (T) -> K): Map<K, Int> {
        val map = mutableMapOf<K, Int>()
        for (item in this) {
            val key = selector(item)
            map.addOrPut(key, 1)
        }
        return map
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

    /**
     * Returns a sublist of this list, starting after the first occurrence of the specified element.
     *
     * @param after The element after which the sublist should start.
     * @param skip The number of elements to skip after the occurrence of `after` (default is 1).
     * @param amount The number of elements to include in the returned sublist (default is 1).
     * @return A list containing up to `amount` elements starting `skip` elements after the first occurrence of `after`,
     *         or an empty list if `after` is not found.
     */
    fun <T> List<T>.sublistAfter(after: T, skip: Int = 1, amount: Int = 1): List<T> {
        val startIndex = indexOf(after)
        if (startIndex == -1) return emptyList()

        return this.drop(startIndex + skip).take(amount)
    }

    /**
     * Returns a sublist of this list, starting after the first occurrence that matches the condition.
     *
     * @param conditionAfter The element's condition after which the sublist should start.
     * @param skip The number of elements to skip after the occurrence of `after` (default is 1).
     * @param amount The number of elements to include in the returned sublist (default is 1).
     * @return A list containing up to `amount` elements starting `skip` elements after the first occurrence of `after`,
     *         or an empty list if `after` is not found.
     */
    fun <T> List<T>.sublistAfter(conditionAfter: (T) -> Boolean, skip: Int = 1, amount: Int = 1): List<T> {
        val startIndex = indexOfFirst { conditionAfter(it) }
        if (startIndex == -1) return emptyList()

        return this.drop(startIndex + skip).take(amount)
    }

    inline fun <reified T, reified K : MutableList<T>> K.transformAt(index: Int, transform: T.() -> T): K {
        this[index] = transform(this[index])
        return this
    }

    fun <T> MutableList<T>.addNotNull(element: T?) = element?.let { add(it) }

    fun <T> MutableList<T>.addAll(vararg elements: T) = addAll(elements.asList())

    @Deprecated("use ConcurrentLinkedQueue or Mutex-like alternates", ReplaceWith(""))
    fun <K, V> Map<K, V>.editCopy(function: MutableMap<K, V>.() -> Unit): Map<K, V> = toMutableMap().apply(function)

    @Deprecated("use ConcurrentLinkedQueue or Mutex-like alternates", ReplaceWith(""))
    fun <T> List<T>.editCopy(function: MutableList<T>.() -> Unit): List<T> = toMutableList().apply(function)

    fun <K, V> Map<K, V>.moveEntryToTop(matcher: (Map.Entry<K, V>) -> Boolean): Map<K, V> {
        val entry = entries.find(matcher)
        if (entry != null) {
            val newMap = linkedMapOf(entry.key to entry.value)
            newMap.putAll(this)
            return newMap
        }
        return this
    }

    operator fun IntRange.contains(range: IntRange): Boolean = range.first in this && range.last in this

    fun <K, V : Comparable<V>> List<Pair<K, V>>.sorted(): List<Pair<K, V>> {
        return sortedBy { (_, value) -> value }
    }

    fun <K, V : Comparable<V>> Map<K, V>.sorted(): Map<K, V> {
        return asSequence().sortedBy { (_, value) -> value }.associate { it.toPair() }
    }

    fun <K, V : Comparable<V>> Map<K, V>.sortedDesc(): Map<K, V> {
        return asSequence().sortedByDescending { (_, value) -> value }.associate { it.toPair() }
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

    inline fun <reified T> MutableIterator<T>.removeIf(predicate: (T) -> Boolean) {
        while (hasNext()) {
            if (predicate(next())) this.remove()
        }
    }

    fun <K, V> MutableMap<K, V>.removeIf(predicate: (Map.Entry<K, V>) -> Boolean) = entries.iterator().removeIf(predicate)

    /** Removes the first element that matches the given [predicate] in the list. */
    fun <T> List<T>.removeFirst(predicate: (T) -> Boolean): List<T> {
        val mutableList = this.toMutableList()
        val iterator = mutableList.iterator()
        while (iterator.hasNext()) {
            if (predicate(iterator.next())) {
                iterator.remove()
                break
            }
        }
        return mutableList.toList()
    }

    /** Removes the first element that matches the given [predicate] in the map. */
    fun <K, V> Map<K, V>.removeFirst(predicate: (Map.Entry<K, V>) -> Boolean): Map<K, V> {
        val mutableMap = this.toMutableMap()
        val iterator = mutableMap.entries.iterator()
        while (iterator.hasNext()) {
            if (predicate(iterator.next())) {
                iterator.remove()
                break
            }
        }
        return mutableMap.toMap()
    }

    /** Updates a value if it is present in the set (equals), useful if the newValue is not reference equal with the value in the set */
    inline fun <reified T> MutableSet<T>.refreshReference(newValue: T) = if (this.contains(newValue)) {
        this.remove(newValue)
        this.add(newValue)
        true
    } else false

    fun <T> List<T?>.takeIfAllNotNull(): List<T>? {
        val list = mutableListOf<T>()
        for (item in this) {
            if (item != null) list.add(item)
            else return null
        }
        return list
    }

    fun <T, C : Collection<T>> C.takeIfNotEmpty(): C? = takeIf { it.isNotEmpty() }

    fun <K, V> Map<K, V>.takeIfNotEmpty(): Map<K, V>? = takeIf { it.isNotEmpty() }

    fun <T> List<T>.toPair(): Pair<T, T>? = if (size == 2) this[0] to this[1] else null

    fun <T> Pair<T, T>.equalsIgnoreOrder(other: Pair<T, T>) = this.toSet() == other.toSet()

    fun <T> Pair<T, T>.toSet(): Set<T> = setOf(first, second)

    inline fun <reified K : Enum<K>, V> enumMapOf(): EnumMap<K, V> {
        return EnumMap<K, V>(K::class.java)
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

    fun <T> Collection<T>.distribute(subs: Int = 2): List<List<T>> {
        return this.split(ceil(this.size.toDouble() / subs.toDouble()).toInt())
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

    inline fun <T, C : Number, D : Number, R : Number> Iterable<T>.sumOfPair(
        crossinline selector: (T) -> Pair<C, D>,
        crossinline resultConverter: (Double) -> R,
    ): Pair<R, R> {
        var sumFirst = 0.0
        var sumSecond = 0.0

        for (element in this) {
            val (c, d) = selector(element)
            sumFirst += c.toDouble()
            sumSecond += d.toDouble()
        }

        return resultConverter(sumFirst) to resultConverter(sumSecond)
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

    inline fun <reified C : Collection<String>> C.filterNotEmptyString(): C =
        filter { it.isNotEmpty() } as C

    inline fun <reified C : Collection<T>, T : Collection<T2>, T2> C.filterNotEmpty(): C =
        filter { it.isNotEmpty() } as C

    fun <K, V : Any> Map<K?, V>.filterNotNullKeys(): Map<K, V> {
        @Suppress("UNCHECKED_CAST")
        return filterKeys { it != null } as Map<K, V>
    }

    fun <K, V> Map<K, V>.containsKeys(vararg keys: K) = keys.all { this.keys.contains(it) }

    /**
     * Inserts the element at the index or appends it to the end if out of bounds of the list.
     *
     * @param index index to insert at, or append if >= size
     * @param element element to insert or add
     */
    fun <E> MutableList<E>.addOrInsert(index: Int, element: E) {
        if (index < size) add(index, element) else add(element)
    }

    fun <T> Iterable<T>.singleOrDefault(defaultValue: T): T {
        return singleOrNull() ?: defaultValue
    }

    fun <K, V> MutableMap<K, V>.add(pair: Pair<K, V>) {
        this[pair.first] = pair.second
    }

    fun <K, V> MutableMap<K, V>.addAll(vararg pairs: Pair<K, V>) {
        for (pair in pairs) {
            this[pair.first] = pair.second
        }
    }

    @Deprecated("Use the removeIf function provided by java")
    fun <T> MutableList<T>.removeIf(predicate: (T) -> Boolean) = removeIf(predicate)

    fun <K, V> MutableMap<K, V>.removeIfKey(predicate: (K) -> Boolean) {
        val iterator = this.entries.iterator()
        while (iterator.hasNext()) {
            if (predicate(iterator.next().key)) {
                iterator.remove()
            }
        }
    }

    fun <K, V> LinkedHashMap<K, V>.putAt(index: Int, key: K, value: V) {
        val entries = LinkedHashMap<K, V>()
        var currentIndex = 0

        for ((existingKey, existingValue) in this) {
            if (currentIndex == index) {
                entries[key] = value // Insert at the specified index
            }
            entries[existingKey] = existingValue
            currentIndex++
        }

        if (index >= size) {
            entries[key] = value // If index is out of range, append at the end
        }

        clear()
        putAll(entries)
    }

    fun <T> Collection<T>.indexOfFirstOrNull(predicate: (T) -> Boolean): Int? {
        return indexOfFirst(predicate).takeIf { it != -1 }
    }

    class OrderedQueue<T> : PriorityQueue<WeightedItem<T>>() {
        fun add(item: T, weight: Double): Boolean = super.add(WeightedItem(item, weight))
        fun copyWithFilter(predicate: (T) -> Boolean): OrderedQueue<T> {
            val newQueue = OrderedQueue<T>()
            for (item in this) {
                if (!predicate(item.item)) {
                    newQueue.add(item.item, item.weight)
                }
            }
            return newQueue
        }

        fun pollOrNull(): T? = poll()?.item
        fun getWaitingWeightOrNull(): Double? = peek()?.weight
    }

    data class WeightedItem<T>(val item: T, val weight: Double) : Comparable<WeightedItem<T>> {
        override fun compareTo(other: WeightedItem<T>): Int = this.weight.compareTo(other.weight)
    }

    class ObservableMutableMap<K, V>(
        private val map: MutableMap<K, V> = mutableMapOf(),
        private val preUpdate: (K, V?) -> Unit = { _, _ -> },
        private val postUpdate: (K, V?) -> Unit = { _, _ -> },
    ) : MutableMap<K, V> by map {

        override fun put(key: K, value: V): V? {
            preUpdate(key, value)
            val oldValue = map.put(key, value)
            postUpdate(key, value)
            return oldValue
        }

        override fun remove(key: K): V? {
            preUpdate(key, null)
            val removedValue = map.remove(key)
            postUpdate(key, null)
            return removedValue
        }
    }

    fun <K> MutableMap<K, SimpleTimeMark>.evictOldestEntry(cap: Int) {
        if (size <= cap) return
        val oldestKey = minByOrNull { it.value }?.key ?: return
        remove(oldestKey)
    }

    fun <T> Collection<T>.firstUniqueByOrNull(
        vararg predicates: (T) -> Boolean,
    ): T? {
        var candidates = this
        for (pred in predicates) {
            val next = candidates.filter(pred)
            if (next.isEmpty()) return null
            else if (next.size == 1) return next.single()
            candidates = next
        }
        return null
    }

    /**
     * Insert content after a line that matches the given pattern.
     *
     * @param pattern the pattern to match
     * @param content the content to insert
     */
    fun MutableList<String>.insertLineAfter(pattern: Pattern, content: String) {
        val iter = this.listIterator()
        while (iter.hasNext()) {
            val line = iter.next()
            if (pattern.matcher(line).find()) {
                iter.add(content)
            }
        }
    }

    // remove every element in MutableList that is not in the Sequence
    fun <T> MutableList<T>.keepOnlyIn(sequence: Sequence<T>) {
        retainAll(sequence.toSet())
    }

    fun <T> Set<T>.optionalEmpty(): Set<T> = if (isEmpty()) emptySet() else this

}
