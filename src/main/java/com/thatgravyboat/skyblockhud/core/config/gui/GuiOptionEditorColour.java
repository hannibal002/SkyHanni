package com.thatgravyboat.skyblockhud.core.config.gui;

import static com.thatgravyboat.skyblockhud.GuiTextures.*;

import com.thatgravyboat.skyblockhud.core.ChromaColour;
import com.thatgravyboat.skyblockhud.core.GuiElementColour;
import com.thatgravyboat.skyblockhud.core.config.struct.ConfigProcessor;
import com.thatgravyboat.skyblockhud.core.util.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

public class GuiOptionEditorColour extends GuiOptionEditor {

    private String chromaColour;
    private GuiElementColour colourElement = null;

    public GuiOptionEditorColour(ConfigProcessor.ProcessedOption option) {
        super(option);
        this.chromaColour = (String) option.get();
    }

    @Override
    public void render(int x, int y, int width) {
        super.render(x, y, width);
        int height = getHeight();

        int argb = ChromaColour.specialToChromaRGB(chromaColour);
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        GlStateManager.color(r / 255f, g / 255f, b / 255f, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(button_white);
        RenderUtils.drawTexturedRect(
            x + width / 6f - 24,
            y + height - 7 - 14,
            48,
            16
        );
    }

    @Override
    public void renderOverlay(int x, int y, int width) {
        if (colourElement != null) {
            colourElement.render();
        }
    }

    @Override
    public boolean mouseInputOverlay(
        int x,
        int y,
        int width,
        int mouseX,
        int mouseY
    ) {
        return (
            colourElement != null && colourElement.mouseInput(mouseX, mouseY)
        );
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        int height = getHeight();

        if (
            Mouse.getEventButtonState() &&
            Mouse.getEventButton() == 0 &&
            mouseX > x + width / 6 - 24 &&
            mouseX < x + width / 6 + 24 &&
            mouseY > y + height - 7 - 14 &&
            mouseY < y + height - 7 + 2
        ) {
            colourElement =
                new GuiElementColour(
                    mouseX,
                    mouseY,
                    (String) option.get(),
                    val -> {
                        option.set(val);
                        chromaColour = val;
                    },
                    () -> colourElement = null
                );
        }

        return false;
    }

    @Override
    public boolean keyboardInput() {
        return colourElement != null && colourElement.keyboardInput();
    }
}
