package at.hannibal2.skyhanni.config.migration

sealed interface LoadResult<out T> {
    fun <V> map(mapper: (T?) -> V?): LoadResult<V> {
        if (this is Instance<T>) {
            return Instance(mapper(this.instance))
        }
        return this as LoadResult<V>
    }

    fun or(other: LoadResult<@UnsafeVariance T>): LoadResult<T>

    data class Instance<T>(val instance: T?) : LoadResult<T> {
        override fun or(other: LoadResult<T>): LoadResult<T> {
            return this
        }
    }

    data class Failure(val exception: Throwable, val path: ResolutionPath) : LoadResult<Nothing> {
        override fun or(other: LoadResult<Nothing>): LoadResult<Nothing> {
            if (other is Invalid || other is Failure) return this
            return other
        }
    }

    object UseDefault : LoadResult<Nothing> {
        override fun or(other: LoadResult<Nothing>): LoadResult<Nothing> {
            if (other is Instance) return other
            return this
        }
    }

    object Invalid : LoadResult<Nothing> {
        override fun or(other: LoadResult<Nothing>): LoadResult<Nothing> {
            return other
        }
    }
}