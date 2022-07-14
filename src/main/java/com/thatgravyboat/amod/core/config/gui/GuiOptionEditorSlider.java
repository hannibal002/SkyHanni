package com.thatgravyboat.amod.core.config.gui;

import com.thatgravyboat.amod.core.GuiElementTextField;
import com.thatgravyboat.amod.core.config.struct.ConfigProcessor;
import com.thatgravyboat.amod.core.util.GuiElementSlider;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class GuiOptionEditorSlider extends GuiOptionEditor {

    private final GuiElementSlider slider;
    private final GuiElementTextField textField;

    public GuiOptionEditorSlider(ConfigProcessor.ProcessedOption option, float minValue, float maxValue, float minStep) {
        super(option);
        if (minStep < 0) minStep = 0.01f;

        float floatVal = ((Number) option.get()).floatValue();
        {
            String strVal;
            if (floatVal % 1 == 0) {
                strVal = Integer.toString((int) floatVal);
            } else {
                strVal = Float.toString(floatVal);
            }
            textField = new GuiElementTextField(strVal, GuiElementTextField.NO_SPACE | GuiElementTextField.NUM_ONLY | GuiElementTextField.SCALE_TEXT);
        }

        slider =
            new GuiElementSlider(
                0,
                0,
                80,
                minValue,
                maxValue,
                minStep,
                floatVal,
                val -> {
                    option.set(val);

                    String strVal;
                    if (val % 1 == 0) {
                        strVal = Integer.toString(val.intValue());
                    } else {
                        strVal = Float.toString(val);
                        strVal = strVal.replaceAll("(\\.\\d\\d\\d)(?:\\d)+", "$1");
                        strVal = strVal.replaceAll("0+$", "");
                    }
                    textField.setText(strVal);
                }
            );
    }

    @Override
    public void render(int x, int y, int width) {
        super.render(x, y, width);
        int height = getHeight();

        int fullWidth = Math.min(width / 3 - 10, 80);
        int sliderWidth = (fullWidth - 5) * 3 / 4;
        int textFieldWidth = (fullWidth - 5) / 4;

        slider.x = x + width / 6 - fullWidth / 2;
        slider.y = y + height - 7 - 14;
        slider.width = sliderWidth;
        slider.render();

        if (textField.getFocus()) {
            textField.setOptions(GuiElementTextField.NO_SPACE | GuiElementTextField.NUM_ONLY);
            textField.setSize(Minecraft.getMinecraft().fontRendererObj.getStringWidth(textField.getText()) + 10, 16);
        } else {
            textField.setSize(textFieldWidth, 16);
            textField.setOptions(GuiElementTextField.NO_SPACE | GuiElementTextField.NUM_ONLY | GuiElementTextField.SCALE_TEXT);
        }

        textField.render(x + width / 6 - fullWidth / 2 + sliderWidth + 5, y + height - 7 - 14);
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        int height = getHeight();

        int fullWidth = Math.min(width / 3 - 10, 80);
        int sliderWidth = (fullWidth - 5) * 3 / 4;
        int textFieldWidth = (fullWidth - 5) / 4;

        slider.x = x + width / 6 - fullWidth / 2;
        slider.y = y + height - 7 - 14;
        slider.width = sliderWidth;
        if (slider.mouseInput(mouseX, mouseY)) {
            textField.unfocus();
            return true;
        }

        if (textField.getFocus()) {
            textFieldWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(textField.getText()) + 10;
        }

        int textFieldX = x + width / 6 - fullWidth / 2 + sliderWidth + 5;
        int textFieldY = y + height - 7 - 14;
        textField.setSize(textFieldWidth, 16);

        if (Mouse.getEventButtonState() && (Mouse.getEventButton() == 0 || Mouse.getEventButton() == 1)) {
            if (mouseX > textFieldX && mouseX < textFieldX + textFieldWidth && mouseY > textFieldY && mouseY < textFieldY + 16) {
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
            textField.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());

            try {
                textField.setCustomBorderColour(0xffffffff);
                float f = Float.parseFloat(textField.getText());
                if (option.set(f)) {
                    slider.setValue(f);
                } else {
                    textField.setCustomBorderColour(0xff0000ff);
                }
            } catch (Exception e) {
                textField.setCustomBorderColour(0xffff0000);
            }

            return true;
        }
        return false;
    }
}
