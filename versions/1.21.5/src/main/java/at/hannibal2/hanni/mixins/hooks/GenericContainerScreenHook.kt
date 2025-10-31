package at.hannibal2.hanni.mixins.hooks

import at.hannibal2.hanni.features.inventory.BetterContainers
import net.minecraft.util.Identifier

class GenericContainerScreenHook {
    fun getTexture(sprite: Identifier): Identifier = BetterContainers.getTextureIdentifier(sprite)
}
