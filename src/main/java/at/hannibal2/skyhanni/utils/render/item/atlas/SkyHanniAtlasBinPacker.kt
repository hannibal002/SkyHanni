package at.hannibal2.skyhanni.utils.render.item.atlas

internal class SkyHanniAtlasBinPacker(size: Int) {

    private companion object {
        private const val PADDING = 1
    }

    private val root: Node = Node(0, 0, size, size)

    data class PackedNode(val x: Int, val y: Int)

    private inner class Node(val x: Int, val y: Int, val width: Int, val height: Int) {
        var left: Node? = null
        var right: Node? = null
        var occupied = false

        fun insert(size: Int): Node? {
            if (left != null || right != null) return left?.insert(size) ?: right?.insert(size)
            if (occupied || size > width || size > height) return null
            if (size == width && size == height) {
                occupied = true
                return this
            }

            val dw = width - size
            val dh = height - size
            if (dw > dh) {
                left = Node(x, y, size, height)
                right = Node(x + size + PADDING, y, width - size - PADDING, height)
            } else {
                left = Node(x, y, width, size)
                right = Node(x, y + size + PADDING, width, height - size - PADDING)
            }
            return left?.insert(size)
        }
    }

    /** Returns the position of the inserted slot, or null if the packer is full. */
    fun insert(size: Int): PackedNode? {
        val node = root.insert(size) ?: return null
        return PackedNode(node.x, node.y)
    }
}
