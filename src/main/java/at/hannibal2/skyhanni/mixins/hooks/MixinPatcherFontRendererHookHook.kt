package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.chroma.ChromaManager
import at.hannibal2.skyhanni.features.misc.EmojiReplacer
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

object MixinPatcherFontRendererHookHook {
    @JvmStatic
    @Suppress("UnusedParameter")
    fun overridePatcherFontRenderer(string: String, shadow: Boolean, cir: CallbackInfoReturnable<Boolean>) {
        if (EmojiReplacer.isEnabled()) {
            var inEmoji = false
            for (i in string.indices) {
                if (string[i] == ':') {
                    if (inEmoji) {
                        cir.returnValue = false
                        return
                    }
                    inEmoji = true
                } else if (string[i] == ' ') {
                    inEmoji = false
                }
            }
        }

        if (!SkyBlockUtils.onHypixel) return

        if (ChromaManager.config.allChroma) {
            cir.returnValue = false
            return
        }

        if (
            ChromaManager.config.allChroma ||
            string == FontRendererHook.chromaPreviewText ||
            string.contains("§#§")
        ) {
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
