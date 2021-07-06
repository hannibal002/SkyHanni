package com.thatgravyboat.skyblockhud.core.config.gui;

import com.thatgravyboat.skyblockhud.core.GuiElementBoolean;
import com.thatgravyboat.skyblockhud.core.GuiElementTextField;
import com.thatgravyboat.skyblockhud.core.config.struct.ConfigProcessor;
import com.thatgravyboat.skyblockhud.core.util.GuiElementSlider;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class GuiOptionEditorText extends GuiOptionEditor {

    private final GuiElementTextField textField;

    public GuiOptionEditorText(ConfigProcessor.ProcessedOption option) {
        super(option);
        textField = new GuiElementTextField((String) option.get(), 0);
    }

    @Override
    public void render(int x, int y, int width) {
        super.render(x, y, width);
        int height = getHeight();

        int fullWidth = Math.min(width / 3 - 10, 80);

        int textFieldX = x + width / 6 - fullWidth / 2;
        if (textField.getFocus()) {
            fullWidth =
                Math.max(
                    fullWidth,
                    Minecraft
                        .getMinecraft()
                        .fontRendererObj.getStringWidth(textField.getText()) +
                    10
                );
        }

        textField.setSize(fullWidth, 16);

        textField.render(textFieldX, y + height - 7 - 14);
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        int height = getHeight();

        int fullWidth = Math.min(width / 3 - 10, 80);

        int textFieldX = x + width / 6 - fullWidth / 2;

        if (textField.getFocus()) {
            fullWidth =
                Math.max(
                    fullWidth,
                    Minecraft
                        .getMinecraft()
                        .fontRendererObj.getStringWidth(textField.getText()) +
                    10
                );
        }

        int textFieldY = y + height - 7 - 14;
        textField.setSize(fullWidth, 16);

        if (
            Mouse.getEventButtonState() &&
            (Mouse.getEventButton() == 0 || Mouse.getEventButton() == 1)
        ) {
            if (
                mouseX > textFieldX &&
                mouseX < textFieldX + fullWidth &&
                mouseY > textFieldY &&
                mouseY < textFieldY + 16
            ) {
                textField.mouseClicked(mouseX, mouseY, Mouse.getEventButton());
                return true;
            }
            textField.unfocus();
        }

        return false;
    }

    @Override
    public boolean keyboardInput() {
        if (Keyboard.getEventKeyState() && textField.getFocus()) {
            Keyboard.enableRepeatEvents(true);
            textField.keyTyped(
                Keyboard.getEventCharacter(),
                Keyboard.getEventKey()
            );

            try {
                textField.setCustomBorderColour(0xffffffff);
                option.set(textField.getText());
            } catch (Exception e) {
                textField.setCustomBorderColour(0xffff0000);
            }

            return true;
        }
        return false;
    }
}
