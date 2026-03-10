package at.hannibal2.skyhanni.utils.render.states

/**
 * Captures all pose-derived shader parameters for a rounded rect at the point of
 * submission, so the render state classes themselves stay agnostic of the shader implementations.
 * All values are pre-adjusted for the current pose matrix.
 */
data class RoundedRenderStateParams(
    val radius: Float,
    val adjustedHalfSizeX: Float,
    val adjustedHalfSizeY: Float,
    val adjustedCenterPosX: Float,
    val adjustedCenterPosY: Float,
    val matXScale: Float,
    val matYScale: Float,
    val matXTranslation: Float,
    val matYTranslation: Float,
)
