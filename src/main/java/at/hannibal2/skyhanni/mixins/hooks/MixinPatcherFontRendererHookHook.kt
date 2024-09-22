package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.chroma.ChromaManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

object MixinPatcherFontRendererHookHook {
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun overridePatcherFontRenderer(string: String, shadow: Boolean, cir: CallbackInfoReturnable<Boolean>) {
        if (!LorenzUtils.onHypixel) return

        if (ChromaManager.config.enabled.get()) {
            cir.cancel()
            cir.returnValue = false
        }
    }
}
