package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.inventory.BetterContainers
import net.minecraft.util.Identifier

class GenericContainerScreenHook {
    fun getTexture(sprite: Identifier): Identifier = BetterContainers.getTextureIdentifier(sprite)
}
