package at.hannibal2.skyhanni.utils.render

/** A short-lived opacity override used while a world item model submits its render nodes. */
object ItemRenderTransparency {

    private val alphaOverride = ThreadLocal<Int?>()

    @JvmStatic
    fun getAlphaOverride(): Int? = alphaOverride.get()

    fun withOpacity(opacity: Float, block: () -> Unit) {
        alphaOverride.set((opacity * 255).toInt().coerceIn(0, 255))
        try {
            block()
        } finally {
            alphaOverride.remove()
        }
    }
}
