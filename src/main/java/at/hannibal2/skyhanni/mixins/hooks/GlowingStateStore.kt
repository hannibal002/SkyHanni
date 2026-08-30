package at.hannibal2.skyhanni.mixins.hooks

interface GlowingStateStore {
    // Naming is intentional
    @Suppress("FunctionName")
    fun `skyhanni$isUsingCustomOutline`(): Boolean = throw UnsupportedOperationException("Implemented via mixin")

    // Naming is intentional
    @Suppress("FunctionName")
    fun `skyhanni$setUsingCustomOutline`() {
        throw UnsupportedOperationException("Implemented via mixin")
    }
}
