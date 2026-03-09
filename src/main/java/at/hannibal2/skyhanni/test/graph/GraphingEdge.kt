package at.hannibal2.skyhanni.test.graph

import java.awt.Color

class GraphingEdge(val node1: GraphingNode, val node2: GraphingNode, var direction: EdgeDirection = EdgeDirection.BOTH) {

    var networkColor: Color? = null

    fun isInEdge(node: GraphingNode?) = node1 == node || node2 == node

    fun getOther(node: GraphingNode): GraphingNode = if (node == node1) node2 else node1

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GraphingEdge

        return (this.node1 == other.node1 && this.node2 == other.node2) || (this.node1 == other.node2 && this.node2 == other.node1)
    }

    override fun hashCode(): Int {
        val hash1 = node1.hashCode()
        val hash2 = node2.hashCode()

        var result: Int
        if (hash1 <= hash2) {
            result = hash1
            result = 31 * result + hash2
        } else {
            result = hash2
            result = 31 * result + hash1
        }
        return result
    }

    fun cycleDirection(standpoint: GraphingNode?) {
        direction = if (standpoint != node2) {
            when (direction) {
                EdgeDirection.BOTH -> EdgeDirection.ONE_TO_TWO
                EdgeDirection.ONE_TO_TWO -> EdgeDirection.TWO_TO_ONE
                EdgeDirection.TWO_TO_ONE -> EdgeDirection.BOTH
            }
        } else {
            when (direction) {
                EdgeDirection.BOTH -> EdgeDirection.TWO_TO_ONE
                EdgeDirection.TWO_TO_ONE -> EdgeDirection.ONE_TO_TWO
                EdgeDirection.ONE_TO_TWO -> EdgeDirection.BOTH
            }
        }
    }

    fun cycleText(standpoint: GraphingNode?) = when (direction) {
        EdgeDirection.BOTH -> "Bidirectional"
        EdgeDirection.ONE_TO_TWO -> if (standpoint != node1) {
            "AwayFromYou"
        } else {
            "ToYou"
        }

        EdgeDirection.TWO_TO_ONE -> if (standpoint != node1) {
            "ToYou"
        } else {
            "AwayFromYou"
        }
    }

    fun isValidDirectionFrom(standpoint: GraphingNode?) = when (direction) {
        EdgeDirection.BOTH -> true
        EdgeDirection.ONE_TO_TWO -> standpoint == node1
        EdgeDirection.TWO_TO_ONE -> standpoint == node2
    }

    fun isValidConnectionFromTo(a: GraphingNode?, b: GraphingNode?): Boolean =
        ((this.node1 == a && this.node2 == b) || (this.node1 == b && this.node2 == a)) && isValidDirectionFrom(a)
}
