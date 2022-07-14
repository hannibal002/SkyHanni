package at.hannibal2.skyhanni.config.core.config.gui;

import static at.hannibal2.skyhanni.config.GuiTextures.DELETE;
import static at.hannibal2.skyhanni.config.GuiTextures.button_tex;

import at.hannibal2.skyhanni.config.core.config.struct.ConfigProcessor;
import at.hannibal2.skyhanni.config.utils.Utils;
import at.hannibal2.skyhanni.config.core.util.lerp.LerpUtils;
import at.hannibal2.skyhanni.config.core.util.render.RenderUtils;
import at.hannibal2.skyhanni.config.core.util.render.TextRenderUtils;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiOptionEditorDraggableList extends GuiOptionEditor {

    private final String[] exampleText;
    private final List<Integer> activeText;
    private int currentDragging = -1;
    private int dragStartIndex = -1;

    private long trashHoverTime = -1;

    private int dragOffsetX = -1;
    private int dragOffsetY = -1;

    private boolean dropdownOpen = false;

    public GuiOptionEditorDraggableList(ConfigProcessor.ProcessedOption option, String[] exampleText) {
        super(option);
        this.exampleText = exampleText;
        this.activeText = (List<Integer>) option.get();
    }

    @Override
    public int getHeight() {
        int height = super.getHeight() + 13;

        for (int strIndex : activeText) {
            String str = exampleText[strIndex];
            height += 10 * str.split("\n").length;
        }

        return height;
    }

    @Override
    public void render(int x, int y, int width) {
        super.render(x, y, width);

        int height = getHeight();

        GlStateManager.color(1, 1, 1, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(button_tex);
        RenderUtils.drawTexturedRect(x + width / 6 - 24, y + 45 - 7 - 14, 48, 16);

        TextRenderUtils.drawStringCenteredScaledMaxWidth("Add", Minecraft.getMinecraft().fontRendererObj, x + width / 6, y + 45 - 7 - 6, false, 44, 0xFF303030);

        long currentTime = System.currentTimeMillis();
        if (trashHoverTime < 0) {
            float greenBlue = LerpUtils.clampZeroOne((currentTime + trashHoverTime) / 250f);
            GlStateManager.color(1, greenBlue, greenBlue, 1);
        } else {
            float greenBlue = LerpUtils.clampZeroOne((250 + trashHoverTime - currentTime) / 250f);
            GlStateManager.color(1, greenBlue, greenBlue, 1);
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(DELETE);
        Utils.drawTexturedRect(x + width / 6 + 27, y + 45 - 7 - 13, 11, 14, GL11.GL_NEAREST);

        Gui.drawRect(x + 5, y + 45, x + width - 5, y + height - 5, 0xffdddddd);
        Gui.drawRect(x + 6, y + 46, x + width - 6, y + height - 6, 0xff000000);

        int i = 0;
        int yOff = 0;
        for (int strIndex : activeText) {
            String str = exampleText[strIndex];

            String[] multilines = str.split("\n");

            int ySize = multilines.length * 10;

            if (i++ != dragStartIndex) {
                for (int multilineIndex = 0; multilineIndex < multilines.length; multilineIndex++) {
                    String line = multilines[multilineIndex];
                    Utils.drawStringScaledMaxWidth(line + EnumChatFormatting.RESET, Minecraft.getMinecraft().fontRendererObj, x + 20, y + 50 + yOff + multilineIndex * 10, true, width - 20, 0xffffffff);
                }
                Minecraft.getMinecraft().fontRendererObj.drawString("\u2261", x + 10, y + 50 + yOff + ySize / 2 - 4, 0xffffff, true);
            }

            yOff += ySize;
        }
    }

    @Override
    public void renderOverlay(int x, int y, int width) {
        super.renderOverlay(x, y, width);

        if (dropdownOpen) {
            List<Integer> remaining = new ArrayList<>();
            for (int i = 0; i < exampleText.length; i++) {
                remaining.add(i);
            }
            remaining.removeAll(activeText);

            FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
            int dropdownWidth = Math.min(width / 2 - 10, 150);
            int left = dragOffsetX;
            int top = dragOffsetY;

            int dropdownHeight = -1 + 12 * remaining.size();

            int main = 0xff202026;
            int outline = 0xff404046;
            Gui.drawRect(left, top, left + 1, top + dropdownHeight, outline); //Left
            Gui.drawRect(left + 1, top, left + dropdownWidth, top + 1, outline); //Top
            Gui.drawRect(left + dropdownWidth - 1, top + 1, left + dropdownWidth, top + dropdownHeight, outline); //Right
            Gui.drawRect(left + 1, top + dropdownHeight - 1, left + dropdownWidth - 1, top + dropdownHeight, outline); //Bottom
            Gui.drawRect(left + 1, top + 1, left + dropdownWidth - 1, top + dropdownHeight - 1, main); //Middle

            int dropdownY = -1;
            for (int strIndex : remaining) {
                String str = exampleText[strIndex];
                if (str.isEmpty()) {
                    str = "<NONE>";
                }
                TextRenderUtils.drawStringScaledMaxWidth(str.replaceAll("(\n.*)+", " ..."), fr, left + 3, top + 3 + dropdownY, false, dropdownWidth - 6, 0xffa0a0a0);
                dropdownY += 12;
            }
        } else if (currentDragging >= 0) {
            int opacity = 0x80;
            long currentTime = System.currentTimeMillis();
            if (trashHoverTime < 0) {
                float greenBlue = LerpUtils.clampZeroOne((currentTime + trashHoverTime) / 250f);
                opacity = (int) (opacity * greenBlue);
            } else {
                float greenBlue = LerpUtils.clampZeroOne((250 + trashHoverTime - currentTime) / 250f);
                opacity = (int) (opacity * greenBlue);
            }

            if (opacity < 20) return;

            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            int mouseX = Mouse.getX() * scaledResolution.getScaledWidth() / Minecraft.getMinecraft().displayWidth;
            int mouseY = scaledResolution.getScaledHeight() - Mouse.getY() * scaledResolution.getScaledHeight() / Minecraft.getMinecraft().displayHeight - 1;

            String str = exampleText[currentDragging];

            String[] multilines = str.split("\n");

            GlStateManager.enableBlend();
            for (int multilineIndex = 0; multilineIndex < multilines.length; multilineIndex++) {
                String line = multilines[multilineIndex];
                Utils.drawStringScaledMaxWidth(line + EnumChatFormatting.RESET, Minecraft.getMinecraft().fontRendererObj, dragOffsetX + mouseX + 10, dragOffsetY + mouseY + multilineIndex * 10, true, width - 20, 0xffffff | (opacity << 24));
            }

            int ySize = multilines.length * 10;

            Minecraft.getMinecraft().fontRendererObj.drawString("\u2261", dragOffsetX + mouseX, dragOffsetY + mouseY + ySize / 2 - 4, 0xffffff, true);
        }
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        if (!Mouse.getEventButtonState() && !dropdownOpen && dragStartIndex >= 0 && Mouse.getEventButton() == 0 && mouseX >= x + width / 6 + 27 - 3 && mouseX <= x + width / 6 + 27 + 11 + 3 && mouseY >= y + 45 - 7 - 13 - 3 && mouseY <= y + 45 - 7 - 13 + 14 + 3) {
            activeText.remove(dragStartIndex);
            currentDragging = -1;
            dragStartIndex = -1;
            return false;
        }

        if (!Mouse.isButtonDown(0) || dropdownOpen) {
            currentDragging = -1;
            dragStartIndex = -1;
            if (trashHoverTime > 0) trashHoverTime = -System.currentTimeMillis();
        } else if (currentDragging >= 0 && mouseX >= x + width / 6 + 27 - 3 && mouseX <= x + width / 6 + 27 + 11 + 3 && mouseY >= y + 45 - 7 - 13 - 3 && mouseY <= y + 45 - 7 - 13 + 14 + 3) {
            if (trashHoverTime < 0) trashHoverTime = System.currentTimeMillis();
        } else {
            if (trashHoverTime > 0) trashHoverTime = -System.currentTimeMillis();
        }

        if (Mouse.getEventButtonState()) {
            int height = getHeight();

            if (dropdownOpen) {
                List<Integer> remaining = new ArrayList<>();
                for (int i = 0; i < exampleText.length; i++) {
                    remaining.add(i);
                }
                remaining.removeAll(activeText);

                int dropdownWidth = Math.min(width / 2 - 10, 150);
                int left = dragOffsetX;
                int top = dragOffsetY;

                int dropdownHeight = -1 + 12 * remaining.size();

                if (mouseX > left && mouseX < left + dropdownWidth && mouseY > top && mouseY < top + dropdownHeight) {
                    int dropdownY = -1;
                    for (int strIndex : remaining) {
                        if (mouseY < top + dropdownY + 12) {
                            activeText.add(0, strIndex);
                            if (remaining.size() == 1) dropdownOpen = false;
                            return true;
                        }

                        dropdownY += 12;
                    }
                }

                dropdownOpen = false;
                return true;
            }

            if (activeText.size() < exampleText.length && mouseX > x + width / 6 - 24 && mouseX < x + width / 6 + 24 && mouseY > y + 45 - 7 - 14 && mouseY < y + 45 - 7 + 2) {
                dropdownOpen = !dropdownOpen;
                dragOffsetX = mouseX;
                dragOffsetY = mouseY;
                return true;
            }

            if (Mouse.getEventButton() == 0 && mouseX > x + 5 && mouseX < x + width - 5 && mouseY > y + 45 && mouseY < y + height - 6) {
                int yOff = 0;
                int i = 0;
                for (int strIndex : activeText) {
                    int ySize = 10 * exampleText[strIndex].split("\n").length;
                    if (mouseY < y + 50 + yOff + ySize) {
                        dragOffsetX = x + 10 - mouseX;
                        dragOffsetY = y + 50 + yOff - mouseY;

                        currentDragging = strIndex;
                        dragStartIndex = i;
                        break;
                    }
                    yOff += ySize;
                    i++;
                }
            }
        } else if (Mouse.getEventButton() == -1 && currentDragging >= 0) {
            int yOff = 0;
            int i = 0;
            for (int strIndex : activeText) {
                if (dragOffsetY + mouseY + 4 < y + 50 + yOff + 10) {
                    activeText.remove(dragStartIndex);
                    activeText.add(i, currentDragging);

                    dragStartIndex = i;
                    break;
                }
                yOff += 10 * exampleText[strIndex].split("\n").length;
                i++;
            }
        }

        return false;
    }

    @Override
    public boolean keyboardInput() {
        return false;
    }
}
