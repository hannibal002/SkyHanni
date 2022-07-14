package at.hannibal2.skyhanni.config.core.config.gui;

import at.hannibal2.skyhanni.config.core.config.struct.ConfigProcessor;
import at.hannibal2.skyhanni.config.core.util.render.RenderUtils;
import at.hannibal2.skyhanni.config.core.util.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

public abstract class GuiOptionEditor {

    protected final ConfigProcessor.ProcessedOption option;
    private static final int HEIGHT = 45;

    public GuiOptionEditor(ConfigProcessor.ProcessedOption option) {
        this.option = option;
    }

    public void render(int x, int y, int width) {
        int height = getHeight();

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        RenderUtils.drawFloatingRectDark(x, y, width, height, true);
        TextRenderUtils.drawStringCenteredScaledMaxWidth(option.name, fr, x + width / 6, y + 13, true, width / 3 - 10, 0xc0c0c0);

        int maxLines = 5;
        float scale = 1;
        int lineCount = fr.listFormattedStringToWidth(option.desc, width * 2 / 3 - 10).size();

        if (lineCount <= 0) return;

        float paraHeight = 9 * lineCount - 1;

        while (paraHeight >= HEIGHT - 10) {
            scale -= 1 / 8f;
            lineCount = fr.listFormattedStringToWidth(option.desc, (int) (width * 2 / 3 / scale - 10)).size();
            paraHeight = (int) (9 * scale * lineCount - 1 * scale);
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 5 + width / 3f, y + HEIGHT / 2f - paraHeight / 2, 0);
        GlStateManager.scale(scale, scale, 1);

        fr.drawSplitString(option.desc, 0, 0, (int) (width * 2 / 3 / scale - 10), 0xc0c0c0);

        GlStateManager.popMatrix();
    }

    public int getHeight() {
        return HEIGHT;
    }

    public abstract boolean mouseInput(int x, int y, int width, int mouseX, int mouseY);

    public abstract boolean keyboardInput();

    public boolean mouseInputOverlay(int x, int y, int width, int mouseX, int mouseY) {
        return false;
    }

    public void renderOverlay(int x, int y, int width) {}
}
