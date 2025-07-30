package at.hannibal2.skyhanni.utils.renderables.container

import at.hannibal2.skyhanni.utils.renderables.ScrollInput


/**
 * @property windowMin The smallest value allowed inside the window
 * @property windowMax The largest value allowed inside the window
 */
interface SlidingWindow {

    val windowMin: Int
    val windowSize: Int
    val windowMax: Int

    val scroll: ScrollInput

    val lowerBound get() = windowMin
    val upperBound get() = windowMax - windowSize

    /**
     * Min: [windowMin]
     * Max: [windowMax] - [windowSize]
     */
    val windowStart get() = scroll.asInt()

    /**
     * Min: [windowMin] + [windowSize]
     * Max: [windowMax]
     */
    val windowEnd get() = scroll.asInt() + windowSize
}

interface SlidingWindowWithScrollHints : SlidingWindow {

    val showScrollableTipsInList: Boolean
    val scrollUpSize: Int
    val scrollDownSize: Int

    override val windowStart
        get() = if (!showScrollableTipsInList || super.windowStart == windowMin) super.windowStart else {
            super.windowStart + scrollUpSize
        }
    override val windowEnd
        get() = if (!showScrollableTipsInList || super.windowEnd == windowMax) super.windowEnd else {
            super.windowEnd - scrollDownSize
        }
}

/**
 * @param getPostPosition returns the absolute Position of that [P]
 * @return A Range starting with the first element that is inside the window
 * and the last element that fits inside the window size, if the previous
 * ones are stacked on top of the first element
 */
fun <P> SlidingWindow.relativeProvider(getPostPosition: (post: P) -> Int): (elements: Iterable<P>) -> IntRange = lambda@{ elements ->
    val iter = elements.withIndex().iterator()

    if (!iter.hasNext()) return@lambda IntRange.EMPTY
    var next: IndexedValue<P> = iter.next()

    var start = -1
    var end = -1
    val windowStart = windowStart
    var windowEnd = windowEnd

    while (true) {
        val size = getPostPosition(next.value)
        if (size >= windowStart) {
            start = next.index
            windowEnd += size - windowStart
            if (!iter.hasNext()) break
            next = iter.next()
            break
        }
        if (!iter.hasNext()) break
        next = iter.next()
    }
    if (start == -1) return@lambda IntRange.EMPTY

    while (true) {
        val size = getPostPosition(next.value)
        if (size > windowEnd) {
            end = next.index
            break
        }
        if (!iter.hasNext()) break
        next = iter.next()
    }
    if (end == -1) end = next.index + 1

    return@lambda start..(end - 2)
}

/**
 * @param getFencePosition returns the actual Size of that [F]
 * @return A Range starting with the first element that is inside the window
 * and the last element that fits inside the window size, if the previous
 * ones are stacked on top of the first element
 */
fun <F> SlidingWindow.absoluteProvider(getFencePosition: (fence: F) -> Int): (elements: Iterable<F>) -> IntRange {
    var accumulator = windowMin
    val provider = relativeProvider<F?> { fence ->
        accumulator.also { fence?.let { accumulator += getFencePosition(fence) } }
    }
    return {
        accumulator = windowMin
        val sequence = sequence {
            yieldAll(it)
            yield(null)
        }
        provider(sequence.asIterable())
    }
}
