package at.hannibal2.skyhanni.events

interface ErrorLoggableEvent {
    fun thereWasAnError()

    fun wasThereAnError(): Boolean
}
