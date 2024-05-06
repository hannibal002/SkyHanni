package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.chroma.ChromaFontRenderer
import at.hannibal2.skyhanni.features.chroma.ChromaManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import net.minecraft.client.renderer.GlStateManager

/**
 * Object to handle chroma font states from handler methods from MixinFontRenderer
 *
 * Modified from SkyblockAddons
 *
 * Credit: [FontRendererHook.java](https://github.com/BiscuitDevelopment/SkyblockAddons/blob/main/src/main/java/codes/biscuit/skyblockaddons/asm/hooks/FontRendererHook.java)
 */
object FontRendererHook {

    private const val CHROMA_FORMAT_INDEX = 22
    private const val WHITE_FORMAT_INDEX = 15

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
        if (!ChromaManager.config.enabled) return
        if (ChromaManager.config.allChroma && ChromaManager.config.ignoreChat && cameFromChat) {
            endChromaFont()
            return
        }

        if (text == "§fPlease star the mod on GitHub!") {
            previewChroma = true
            setupChromaFont()
        }

        currentDrawState = if (shadow) DRAW_CHROMA_SHADOW else DRAW_CHROMA

        // Best feature ngl
        if (ChromaManager.config.allChroma) {
            // Handles setting the base color of text when they don't use color codes i.e. MoulConfig
            if (shadow) {
                GlStateManager.color(0.33f, 0.33f, 0.33f, RenderUtils.getAlpha())
            } else {
                GlStateManager.color(1f, 1f, 1f, RenderUtils.getAlpha())
            }
            setupChromaFont()
        }

        currentDrawState?.loadChromaEnv()
    }

    @JvmStatic
    fun toggleChromaOn() {
        if (!LorenzUtils.inSkyBlock) return

        currentDrawState?.newChromaEnv()?.bindActualColor(RenderUtils.getAlpha())
    }

    @JvmStatic
    fun forceWhiteColorCode(formatIndex: Int): Int {
        if (!LorenzUtils.inSkyBlock) return formatIndex

        if (!ChromaManager.config.enabled) return formatIndex

        val drawState = currentDrawState ?: return formatIndex
        if (drawState.getChromaState() && formatIndex <= WHITE_FORMAT_INDEX) { // If it's a color code
            return WHITE_FORMAT_INDEX
        }

        return formatIndex
    }

    @JvmStatic
    fun restoreChromaState() {
        if (!LorenzUtils.inSkyBlock) return
        if (!ChromaManager.config.enabled) return

        currentDrawState?.restoreChromaEnv()
    }

    @JvmStatic
    fun endChromaRendering() {
        if (!LorenzUtils.inSkyBlock) return
        if (!ChromaManager.config.enabled) return

        if (previewChroma) {
            previewChroma = false
            endChromaFont()
        }

        if (ChromaManager.config.allChroma) endChromaFont()

        currentDrawState?.endChromaEnv()
    }

    @JvmStatic
    fun insertZColorCode(constant: String): String {
        return if (LorenzUtils.inSkyBlock && !ChromaManager.config.enabled) constant else "0123456789abcdefklmnorz"
    }

    @JvmStatic
    fun toggleChromaAndResetStyle(formatIndex: Int): Boolean {
        if (!LorenzUtils.inSkyBlock) return false
        if (!ChromaManager.config.enabled) return false
        if (formatIndex == CHROMA_FORMAT_INDEX) {
            toggleChromaOn()
            return true
        }
        return false
    }
}
