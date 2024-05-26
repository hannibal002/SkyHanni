package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import net.minecraft.client.renderer.texture.TextureAtlasSprite

private val config get() = SkyHanniMod.feature.fishing.lavaReplacementConfig

fun replaceSprite(lavaSprite: Array<TextureAtlasSprite>, waterSprite: Array<TextureAtlasSprite>): Array<TextureAtlasSprite> {
    if (!LorenzUtils.inSkyBlock || !config.enabled) return lavaSprite

    return if (config.onlyInCrimsonIsle) {
        if (IslandType.CRIMSON_ISLE.isInIsland())
            waterSprite
        else
            lavaSprite
    } else {
        waterSprite
    }
}
