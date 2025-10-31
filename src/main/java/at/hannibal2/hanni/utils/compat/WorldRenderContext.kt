package at.hannibal2.hanni.utils.compat

class WorldRenderContext {
    private val _matrixStack = MatrixStack()
    fun matrixStack() = _matrixStack
}
