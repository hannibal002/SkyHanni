package at.hannibal2.skyhanni.utils

object ExtraOperators {

    operator fun Pair<Int, Int>.plus(new: Pair<Int, Int>): Pair<Int, Int> =
        Pair(this.first + new.first, this.second + new.second)

    operator fun Pair<Int, Int>.minus(new: Pair<Int, Int>): Pair<Int, Int> =
        Pair(this.first - new.first, this.second - new.second)
}
