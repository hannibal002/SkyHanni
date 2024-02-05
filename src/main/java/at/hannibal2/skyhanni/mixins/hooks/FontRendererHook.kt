package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.chroma.ChromaFontRenderer
import at.hannibal2.skyhanni.mixins.transformers.AccessorFontRenderer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.shader.ShaderManager
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Object to handle chroma font states from handler methods from MixinFontRenderer
 *
 * Modified from SkyblockAddons
 *
 * Credit: [FontRendererHook.java](https://github.com/BiscuitDevelopment/SkyblockAddons/blob/main/src/main/java/codes/biscuit/skyblockaddons/asm/hooks/FontRendererHook.java)
 */
object FontRendererHook {
    private var CHROMA_COLOR: Int = -0x1
    private val DRAW_CHROMA = ChromaFontRenderer(CHROMA_COLOR)
    private var CHROMA_COLOR_SHADOW: Int = -0xAAAAAB
    private val DRAW_CHROMA_SHADOW = ChromaFontRenderer(CHROMA_COLOR_SHADOW)

    private var currentDrawState: ChromaFontRenderer? = null
    private var previewChroma = false

    var cameFromChat = false

    /**
     * Setups the [ChromaFontRenderer][at.hannibal2.skyhanni.features.chroma.ChromaFontRenderer] for rendering text
     * in chroma. This should only be used when you don't have control over the color code a string uses, or it
     * doesn't make sense to add §Z color code to a string.
     *
     * If you do have control over the color code, you can prepend the string with §Z instead.
     *
     * **Usage:**
     *
     * Surround string render call with this method and [endChromaFont].
     * ```
     *     FontRendererHook.setupChromaFont()
     *     // render string call(s) here...
     *     FontRendererHook.endChromaFont()
     * ```
     *
     * Note: This only works if the string render call ends up using
     * [FontRenderer#drawString()][net.minecraft.client.gui.FontRenderer.drawString] rather than a custom font renderer
     *
     */
    private fun setupChromaFont() {
        DRAW_CHROMA.startChroma()
        DRAW_CHROMA_SHADOW.startChroma()
    }

    /**
     * See [setupChromaFont]
     */
    private fun endChromaFont() {
        DRAW_CHROMA.endChroma()
        DRAW_CHROMA_SHADOW.endChroma()
    }

    @JvmStatic
    fun beginChromaRendering(text: String, shadow: Boolean) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.chroma.enabled) return
        if (SkyHanniMod.feature.chroma.allChroma && SkyHanniMod.feature.chroma.ignoreChat && cameFromChat) {
            endChromaFont()
            return
        }

        if (text == "§fPlease star the mod on GitHub!") {
            previewChroma = true
            setupChromaFont()
        }

        val alpha = (Minecraft.getMinecraft().fontRendererObj as AccessorFontRenderer).alpha
        if (shadow) {
            currentDrawState = DRAW_CHROMA_SHADOW
            CHROMA_COLOR_SHADOW = ((255 * alpha).toInt() shl 24 or 0x555555)
        } else {
            currentDrawState = DRAW_CHROMA
            CHROMA_COLOR = ((255 * alpha).toInt() shl 24 or 0xFFFFFF)
        }

        // Best feature ngl
        if (SkyHanniMod.feature.chroma.allChroma) {
            // Handles setting the base color of text when they don't use color codes i.e. MoulConfig
            if (shadow) {
                GlStateManager.color(0.33f, 0.33f, 0.33f, 1f)
            } else {
                GlStateManager.color(1f, 1f, 1f, 1f)
            }
            setupChromaFont()
        }

        currentDrawState?.loadChromaEnv()
    }

    @JvmStatic
    fun toggleChromaOn() {
        if (!LorenzUtils.inSkyBlock) return

        currentDrawState?.newChromaEnv()?.bindActualColor()
    }

    @JvmStatic
    fun forceWhiteColorCode(i1: Int): Int {
        if (!LorenzUtils.inSkyBlock) return i1

        if (!SkyHanniMod.feature.chroma.enabled) return i1

        val drawState = currentDrawState ?: return i1
        if (drawState.getChromaState()) {
            if (i1 < 16) {
                return 15
            }
        }

        return i1
    }

    @JvmStatic
    fun restoreChromaState() {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.chroma.enabled) return

        currentDrawState?.restoreChromaEnv()
    }

    @JvmStatic
    fun endChromaRendering() {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.chroma.enabled) return

        if (previewChroma) {
            previewChroma = false
            endChromaFont()
        }

        if (SkyHanniMod.feature.chroma.allChroma) endChromaFont()

        currentDrawState?.endChromaEnv()
    }

    @JvmStatic
    fun insertZColorCode(constant: String): String {
        return if (LorenzUtils.inSkyBlock && !SkyHanniMod.feature.chroma.enabled) constant else "0123456789abcdefklmnorz"
    }

    // TODO add better parameter names
    @JvmStatic
    fun toggleChromaCondition_shouldResetStyles(
        text: String,
        shadow: Boolean,
        ci: CallbackInfo,
        i: Int,
        c0: Char,
        i1: Int
    ): Boolean {
        if (!LorenzUtils.inSkyBlock) return false
        if (!SkyHanniMod.feature.chroma.enabled) return false
        if (i1 == 22) {
            toggleChromaOn()
            return true
        }
        return false
    }
}
