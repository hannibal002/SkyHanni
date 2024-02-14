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
     * leak this instance to any other codepaths which modify the instance. Whenever possible callers should wrap
     * objects they return which offer a view into this object into a [Const] of its own.
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
        fun <T> fromOwned(value: T): Const<T> {
            return Const(value)
        }
    }
}



