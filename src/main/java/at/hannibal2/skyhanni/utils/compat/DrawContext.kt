package at.hannibal2.skyhanni.utils.compat

class DrawContext {
    private val _matrices = MatrixStack()
    val matrices: MatrixStack
        get() = _matrices
}
