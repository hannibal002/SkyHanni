package at.hannibal2.skyhanni.utils.tracker

/**
 * Interface for objects which are resettable using the /shreset command.
 */
interface Resettable {
    val name: String
    fun resetCommand(): Unit
}
