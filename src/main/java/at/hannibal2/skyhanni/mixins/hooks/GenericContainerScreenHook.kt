package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.inventory.BetterContainers
import net.minecraft.resources.ResourceLocation

class GenericContainerScreenHook {
    fun getTexture(sprite: ResourceLocation): ResourceLocation = BetterContainers.getTextureIdentifier(sprite)
}
