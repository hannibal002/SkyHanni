package at.hannibal2.skyhanni.utils.const

@JvmInline
/**
 * Immutable view of an object.
 * This class wraps an [T] indicating that it should not be modified. This allows multiple users to share an
 * object instance without having to fear that the internal mutability of the object causes unexpected behaviour.
 * More specifically, as long as the invariants of the methods and constructors are followed by all users then data
 * contained within will never change.
 *
 * For specific [T]s there are extension methods allowing to safely access the data.
 *
 * This is a [JvmInline] class, so at
 * runtime [Const] is a 0 cost wrapper.
 */
value class Const<T> private constructor(
    /**
     * Unsafely access the underlying object. Callers of this method promise not to modify the returned instance, or to
     * leak this instance or references/views into this instance to any other codepaths which modify it.
     * Whenever possible callers should wrap objects they return which offer a view into this object into a [Const] of
     * its own.
     */
    @PublishedApi
    internal val unsafeMutable: T,
) {
    companion object {
        /**
         * Create a new [Const] instance. Callers of this method guarantee that the given object will not be mutated
         * internally. This should ideally be done by every instance of [value] being wrapped in a [Const] (and other
         * references to be discarded as quickly as possible).
         */
        fun <T> newUnchecked(value: T): Const<T> {
            return Const(value)
        }
    }

    /**
     * Map the contained object into a new [Const].
     */
    inline fun <U> unsafeMap(
        /**
         * This lambda needs to hold the same guarantees for its parameter like for access to [unsafeMutable].
         * It can however assume that the return value will never be modified by other code.
         */
        mapper: (T) -> U
    ): Const<U> {
        return unsafeMutable
            .let(mapper)
            .let(::newUnchecked)
    }

    /**
     * Flat map the contained object into a new [Const]. Behaves like [unsafeMap] but allowing the [mapper] to return a [Const]
     */
    inline fun <U> unsafeFlatMap(
        /**
         * This lambda needs to hold the same guarantees for its parameter like for access to [unsafeMutable].
         */
        mapper: (T) -> Const<U>
    ): Const<U> {
        return unsafeMutable
            .let(mapper)
    }
}

/**
 * Flatten two nested [Const] into just one.
 */
fun <T> Const<Const<T>>.flatten(): Const<T> = unsafeFlatMap { it }

/**
 * Lift nullability out of a [Const] to allow for easier `?.` operations.
 */
fun <T : Any> Const<T?>.liftNull(): Const<T>? = unsafeMutable?.let(Const.Companion::newUnchecked)

inline fun <reified U : T, T> Const<T>.tryCast(): Const<U>? = (unsafeMutable as? U)?.let(Const.Companion::newUnchecked)

/**
 * List a list out of a const. This is legal since [List] does not allow for mutating unless it's elements are individually
 * mutable as long. The caller may never cast the returned instance to [MutableList].
 */
fun <T> Const<List<T>>.liftList(): List<Const<T>> {
    return unsafeMutable.map(Const.Companion::newUnchecked)
}

/**
 * Lift const out of a [List]. The caller must guarantee that the list instance is never modified. This means it was
 * either constructed directly as a [List] or if it originally comes from a [MutableList], mutations operations on this
 * instance are never used.
 */
fun <T> List<Const<T>>.liftConst(): Const<List<T>> {
    return Const.newUnchecked(this.map { it.unsafeMutable })
}
