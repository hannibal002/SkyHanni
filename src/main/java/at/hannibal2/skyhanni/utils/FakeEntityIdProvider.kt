package at.hannibal2.skyhanni.utils

//? if >= 26.2 {
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.fetchAndDecrement

object FakeEntityIdProvider {

    private val nextId = AtomicInt(-1)

    @JvmStatic
    fun getNextId(): Int = nextId.fetchAndDecrement()
}
//?}
