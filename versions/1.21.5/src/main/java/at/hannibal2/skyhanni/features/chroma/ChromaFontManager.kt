package at.hannibal2.skyhanni.features.chroma

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderLayers
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import net.minecraft.client.font.BakedGlyph
import net.minecraft.client.font.BakedGlyph.DrawnGlyph
import net.minecraft.client.font.TextRenderLayerSet
import net.minecraft.client.font.TextRenderer.TextLayerType
import net.minecraft.client.render.RenderLayer
import net.minecraft.text.Style
import net.minecraft.text.TextColor
import net.minecraft.util.Identifier

private var textRenderLayerSetMap: MutableMap<TextRenderLayerSet, TextRenderLayerSet> = mutableMapOf()
var renderingChat: Boolean = false

fun addTextRenderLayerSet(identifier: Identifier, textRenderLayerSet: TextRenderLayerSet) {
    // To render in chroma, we need to use our own RenderLayer that uses a chroma shader.
    // So when Minecraft tries to render a glyph, we intercept the getRenderLayer call
    // and if the current glyph has chroma text color, then we substitute in our custom
    // RenderLayer.

    // However, when rendering glyphs, Minecraft uses a glyph atlas, and these glyph atlases
    // are only 256x256 sized textures, which when filled, Minecraft has to make more. These
    // glyph atlases are stored within the current glyph's RenderLayers.

    // So, when we replace the render layer with our chroma one, we need our render layer to
    // reference the correct glyph atlas, meaning we have to make our render layers with the
    // same glyph atlases as the one used by Minecraft. So how do we know which RenderLayer to
    // use for the current glyph if a glyph could exist in any of the current glyph atlases,
    // and a RenderLayer only refers to 1 glyph atlas?

    // Since the current glyph references the correct glyph atlas that it exists in, we
    // create a map of the TextRenderLayerSet that Minecraft would usually use, to a custom
    // chroma one referencing the same glyph atlas. This way when we intercept the getRenderLayer
    // call, we use the incoming glyph's TextRenderLayerSet to get the corresponding chroma
    // TextRenderLayerSet referencing the correct glyph atlas.

    // (It seems most relevant text and symbols exist in the 'default/' glyph atlases, other glyphs
    // may be rendered to other atlases, but I haven't found what exactly is rendered to them.)
    if (!identifier.toString().startsWith("minecraft:default/")) return
    val chromaTextRenderLayerSet = TextRenderLayerSet(
        SkyHanniRenderLayers.getChromaTextured(identifier),
        SkyHanniRenderLayers.getChromaTextured(identifier),
        SkyHanniRenderLayers.getChromaTextured(identifier),
    )

    // Map Minecraft's TextRenderLayerSet to our custom TextRenderLayerSet
    textRenderLayerSetMap[textRenderLayerSet] = chromaTextRenderLayerSet
}

fun replaceGlyphRenderLayer(instance: BakedGlyph, layerType: TextLayerType, original: Operation<RenderLayer>, drawnGlyph: DrawnGlyph) : RenderLayer {
    val originalRenderLayer = original.call(instance, layerType)
    val enabled = SkyHanniMod.feature.gui.chroma.enabled.get()
    val colorName = drawnGlyph.style.color?.name

    if (enabled && colorName == "chroma") {
        return textRenderLayerSetMap[instance.textRenderLayers]?.getRenderLayer(layerType) ?: originalRenderLayer
    }

    return originalRenderLayer
}

fun setChromaColorStyle(style: Style, text: String, colorCode: Char): Style {
    if (colorCode.lowercaseChar() == 'z') {
        return Style.EMPTY.withColor(TextColor(0xFFFFFF, "chroma"))
    }
    return style
}

fun forceWhiteTextColorForChroma(color: TextColor?): TextColor? {
    if (!SkyHanniMod.feature.gui.chroma.enabled.get()) return color

    val allChroma = SkyHanniMod.feature.gui.chroma.allChroma
    val chatFlag = SkyHanniMod.feature.gui.chroma.ignoreChat && renderingChat

    if (allChroma && !chatFlag) {
        return TextColor(0xFFFFFF, "chroma")
    }
    return color
}

fun forceChromaStyleIfNecessary(style: Style): Style {
    if (!SkyHanniMod.feature.gui.chroma.enabled.get()) return style

    val allChroma = SkyHanniMod.feature.gui.chroma.allChroma
    val chatFlag = SkyHanniMod.feature.gui.chroma.ignoreChat && renderingChat

    if (allChroma && !chatFlag) {
        return style.withColor(TextColor(0xFFFFFE, "chroma"))
    }
    return style
}
