package com.thatgravyboat.skyblockhud.core.config.gui;

import com.thatgravyboat.skyblockhud.core.config.struct.ConfigProcessor;
import com.thatgravyboat.skyblockhud.core.util.render.RenderUtils;
import com.thatgravyboat.skyblockhud.core.util.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiOptionEditorDropdown extends GuiOptionEditor {

    private final String[] values;
    private final boolean useOrdinal;
    private int selected;
    private boolean open = false;

    public GuiOptionEditorDropdown(ConfigProcessor.ProcessedOption option, String[] values, int selected, boolean useOrdinal) {
        super(option);
        if (selected >= values.length) selected = values.length;
        this.values = values;
        this.selected = selected;
        this.useOrdinal = useOrdinal;
    }

    @Override
    public void render(int x, int y, int width) {
        super.render(x, y, width);

        if (!open) {
            int height = getHeight();

            FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
            int dropdownWidth = Math.min(width / 3 - 10, 80);
            int left = x + width / 6 - dropdownWidth / 2;
            int top = y + height - 7 - 14;

            String selectedString = " - Select - ";
            if (selected >= 0 && selected < values.length) {
                selectedString = values[selected];
            }

            RenderUtils.drawFloatingRectDark(left, top, dropdownWidth, 14, false);
            TextRenderUtils.drawStringScaled("\u25BC", fr, left + dropdownWidth - 10, y + height - 7 - 15, false, 0xffa0a0a0, 2);

            TextRenderUtils.drawStringScaledMaxWidth(selectedString, fr, left + 3, top + 3, false, dropdownWidth - 16, 0xffa0a0a0);
        }
    }

    @Override
    public void renderOverlay(int x, int y, int width) {
        if (open) {
            String selectedString = " - Select - ";
            if (selected >= 0 && selected < values.length) {
                selectedString = values[selected];
            }

            int height = getHeight();

            FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
            int dropdownWidth = Math.min(width / 3 - 10, 80);
            int left = x + width / 6 - dropdownWidth / 2;
            int top = y + height - 7 - 14;

            int dropdownHeight = 13 + 12 * values.length;

            int main = 0xff202026;
            int blue = 0xff2355ad;
            Gui.drawRect(left, top, left + 1, top + dropdownHeight, blue); //Left
            Gui.drawRect(left + 1, top, left + dropdownWidth, top + 1, blue); //Top
            Gui.drawRect(left + dropdownWidth - 1, top + 1, left + dropdownWidth, top + dropdownHeight, blue); //Right
            Gui.drawRect(left + 1, top + dropdownHeight - 1, left + dropdownWidth - 1, top + dropdownHeight, blue); //Bottom
            Gui.drawRect(left + 1, top + 1, left + dropdownWidth - 1, top + dropdownHeight - 1, main); //Middle

            Gui.drawRect(left + 1, top + 14 - 1, left + dropdownWidth - 1, top + 14, blue); //Bar

            int dropdownY = 13;
            for (String option : values) {
                if (option.isEmpty()) {
                    option = "<NONE>";
                }
                TextRenderUtils.drawStringScaledMaxWidth(option, fr, left + 3, top + 3 + dropdownY, false, dropdownWidth - 6, 0xffa0a0a0);
                dropdownY += 12;
            }

            TextRenderUtils.drawStringScaled("\u25B2", fr, left + dropdownWidth - 10, y + height - 7 - 15, false, 0xffa0a0a0, 2);

            TextRenderUtils.drawStringScaledMaxWidth(selectedString, fr, left + 3, top + 3, false, dropdownWidth - 16, 0xffa0a0a0);
        }
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        int height = getHeight();

        int left = x + width / 6 - 40;
        int top = y + height - 7 - 14;

        if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0) {
            if (mouseX >= left && mouseX <= left + 80 && mouseY >= top && mouseY <= top + 14) {
                open = !open;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseInputOverlay(int x, int y, int width, int mouseX, int mouseY) {
        int height = getHeight();

        int left = x + width / 6 - 40;
        int top = y + height - 7 - 14;

        if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0) {
            if (!(mouseX >= left && mouseX <= left + 80 && mouseY >= top && mouseY <= top + 14) && open) {
                open = false;
                if (mouseX >= left && mouseX <= left + 80) {
                    int dropdownY = 13;
                    for (int ordinal = 0; ordinal < values.length; ordinal++) {
                        if (mouseY >= top + 3 + dropdownY && mouseY <= top + 3 + dropdownY + 12) {
                            selected = ordinal;
                            if (useOrdinal) {
                                option.set(selected);
                            } else {
                                option.set(values[selected]);
                            }
                            return true;
                        }
                        dropdownY += 12;
                    }
                }
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
