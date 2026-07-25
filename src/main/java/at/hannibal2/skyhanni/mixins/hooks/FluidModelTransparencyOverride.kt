package at.hannibal2.skyhanni.mixins.hooks

//? if >= 26.1 {
import com.mojang.blaze3d.platform.Transparency
import net.minecraft.client.renderer.block.FluidModel

interface FluidModelTransparencyOverride {

    // Naming is intentional
    @Suppress("FunctionName")
    fun `skyhanni$getTransparency`(): Transparency? = throw UnsupportedOperationException("Implemented via mixin")

    @Suppress("FunctionName")
    fun `skyhanni$setTransparency`(value: Transparency?) {
        throw UnsupportedOperationException("Implemented via mixin")
    }

    companion object {

        var FluidModel.transparency: Transparency?
            get() = (this as FluidModelTransparencyOverride).`skyhanni$getTransparency`()
            set(value) {
                (this as FluidModelTransparencyOverride).`skyhanni$setTransparency`(value)
            }
    }
}
//?}
