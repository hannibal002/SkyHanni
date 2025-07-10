package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.chroma.ChromaManager
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

object MixinPatcherFontRendererHookHook {
    @JvmStatic
    @Suppress("UnusedParameter")
    fun overridePatcherFontRenderer(string: String, shadow: Boolean, cir: CallbackInfoReturnable<Boolean>) {
        if (!SkyBlockUtils.onHypixel) return

        if (ChromaManager.config.allChroma) {
            cir.returnValue = false
            return
        }

        if (string == FontRendererHook.chromaPreviewText) {
            cir.returnValue = false
            return
        }

        if (string.contains("§#§")) {
            cir.returnValue = false
            return
        }
        if (ChromaManager.config.enabled.get()) {
            if (string.contains("§z") || string.contains("§Z")) {
                cir.returnValue = false
                return
            }
        }
    }
}
