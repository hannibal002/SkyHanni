package at.hannibal2.skyhanni.utils.compat

class WorldRenderContext {
    private val _matrixStack = MatrixStack()
    fun matrixStack() = _matrixStack
}
