package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.data.model.graph.GraphNode

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
     * A condition that must be passed before navigating to the next node
     * Is checked every second. Is also checked immediately upon first reaching the node
     * If true we move onto the next node and if false we wait until the condition is met
     */
    data class SecondPassed(val condition: (GraphNode) -> Boolean) : NavigationCondition

}
