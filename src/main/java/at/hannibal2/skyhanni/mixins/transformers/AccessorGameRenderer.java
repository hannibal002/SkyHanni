package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface AccessorGameRenderer {
    @Accessor("guiRenderer")
    GuiRenderer getGuiRenderer();

    @Accessor("guiRenderState")
    GuiRenderState getGuiRenderState();
}
