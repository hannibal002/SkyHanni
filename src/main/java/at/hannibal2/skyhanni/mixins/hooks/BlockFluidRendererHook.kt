package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.fishing.LavaReplacementConfig
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColorInt
import net.minecraft.client.renderer.texture.TextureAtlasSprite

private val config get() = SkyHanniMod.feature.fishing.lavaReplacementConfig


fun modifySprite(sprites: Array<TextureAtlasSprite>, water: Array<TextureAtlasSprite>): Array<TextureAtlasSprite> {
    return if (config.renderType.get() == LavaReplacementConfig.RenderType.TEXTURE) water else sprites
}

fun modifyColorMultiplier(i: Int): Int {
    return if (config.renderType.get() == LavaReplacementConfig.RenderType.COLOR) config.color.toChromaColorInt() else i
}
