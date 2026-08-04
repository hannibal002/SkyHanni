package at.hannibal2.skyhanni.features.misc.pathfind

sealed interface NavigationCondition {

    /**
     * Navigate directly to the next node after reaching this one
     */
    data object None : NavigationCondition

    /**
     * A condition that the cleanMessage from the chat event must pass
     */
    data class ChatMessage(val condition: (String) -> Boolean) : NavigationCondition

    /**
     * A condition that the must be passed, is checked every second. Is also checked immediately upon reaching
     */
    data class SecondPassed(val condition: () -> Boolean) : NavigationCondition

}
