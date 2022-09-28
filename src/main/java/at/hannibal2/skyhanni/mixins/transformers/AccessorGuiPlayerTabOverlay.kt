package at.hannibal2.skyhanni.mixins.transformers

import net.minecraft.client.gui.GuiPlayerTabOverlay
import net.minecraft.util.IChatComponent
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(GuiPlayerTabOverlay::class)
interface AccessorGuiPlayerTabOverlay {
    @get:Accessor("footer")
    val footer: IChatComponent?
}