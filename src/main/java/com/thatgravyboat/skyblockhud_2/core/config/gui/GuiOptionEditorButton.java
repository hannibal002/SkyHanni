package com.thatgravyboat.skyblockhud_2.core.config.gui;

import static com.thatgravyboat.skyblockhud_2.GuiTextures.button_tex;

import at.lorenz.mod.config.Features;
import com.thatgravyboat.skyblockhud_2.core.config.struct.ConfigProcessor;
import com.thatgravyboat.skyblockhud_2.core.util.render.RenderUtils;
import com.thatgravyboat.skyblockhud_2.core.util.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

public class GuiOptionEditorButton extends GuiOptionEditor {

    private final String runnableId;
    private String buttonText;
    private final Features config;

    public GuiOptionEditorButton(ConfigProcessor.ProcessedOption option, String runnableId, String buttonText, Features config) {
        super(option);
        this.runnableId = runnableId;
        this.config = config;

        this.buttonText = buttonText;
        if (this.buttonText != null && this.buttonText.isEmpty()) this.buttonText = null;
    }

    @Override
    public void render(int x, int y, int width) {
        super.render(x, y, width);

        int height = getHeight();

        GlStateManager.color(1, 1, 1, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(button_tex);
        RenderUtils.drawTexturedRect(x + width / 6 - 24, y + height - 7 - 14, 48, 16);

        if (buttonText != null) {
            TextRenderUtils.drawStringCenteredScaledMaxWidth(buttonText, Minecraft.getMinecraft().fontRendererObj, x + width / 6, y + height - 7 - 6, false, 44, 0xFF303030);
        }
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        if (Mouse.getEventButtonState()) {
            int height = getHeight();
            if (mouseX > x + width / 6 - 24 && mouseX < x + width / 6 + 24 && mouseY > y + height - 7 - 14 && mouseY < y + height - 7 + 2) {
                config.executeRunnable(runnableId);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean keyboardInput() {
        return false;
    }
}
