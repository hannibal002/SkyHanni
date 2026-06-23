package at.hannibal2.skyhanni.mixins.hooks

//? if >= 26.1 {
import com.mojang.blaze3d.platform.Transparency

// Naming is intentional
@Suppress("FunctionName")
interface FluidModelTransparencyOverride {

    fun `skyhanni$getTransparency`(): Transparency? = throw UnsupportedOperationException("Implemented via mixin")

    fun `skyhanni$setTransparency`(value: Transparency?) {
        throw UnsupportedOperationException("Implemented via mixin")
    }

    // Kotlin-only accessor
    @get:JvmSynthetic
    @set:JvmSynthetic
    var transparency: Transparency?
        get() = `skyhanni$getTransparency`()
        set(value) {
            `skyhanni$setTransparency`(value)
        }
}
//?}
