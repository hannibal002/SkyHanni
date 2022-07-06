package com.thatgravyboat.skyblockhud.core;

import com.thatgravyboat.skyblockhud.GuiTextures;
import com.thatgravyboat.skyblockhud.core.util.lerp.LerpUtils;
import com.thatgravyboat.skyblockhud.core.util.render.RenderUtils;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

public class GuiElementBoolean extends GuiElement {

    public int x;
    public int y;
    private boolean value;
    private int clickRadius;
    private Consumer<Boolean> toggleCallback;

    private boolean previewValue;
    private int animation = 0;
    private long lastMillis = 0;

    private static final int xSize = 48;
    private static final int ySize = 14;

    public GuiElementBoolean(int x, int y, boolean value, Consumer<Boolean> toggleCallback) {
        this(x, y, value, 0, toggleCallback);
    }

    public GuiElementBoolean(int x, int y, boolean value, int clickRadius, Consumer<Boolean> toggleCallback) {
        this.x = x;
        this.y = y;
        this.value = value;
        this.previewValue = value;
        this.clickRadius = clickRadius;
        this.toggleCallback = toggleCallback;
        this.lastMillis = System.currentTimeMillis();

        if (value) animation = 36;
    }

    @Override
    public void render() {
        GlStateManager.color(1, 1, 1, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(GuiTextures.BAR);
        RenderUtils.drawTexturedRect(x, y, xSize, ySize);

        ResourceLocation buttonLoc = GuiTextures.ON;
        long currentMillis = System.currentTimeMillis();
        long deltaMillis = currentMillis - lastMillis;
        lastMillis = currentMillis;
        boolean passedLimit = false;
        if (previewValue != value) {
            if ((previewValue && animation > 12) || (!previewValue && animation < 24)) {
                passedLimit = true;
            }
        }
        if (previewValue != passedLimit) {
            animation += deltaMillis / 10;
        } else {
            animation -= deltaMillis / 10;
        }
        lastMillis -= deltaMillis % 10;

        if (previewValue == value) {
            animation = Math.max(0, Math.min(36, animation));
        } else if (!passedLimit) {
            if (previewValue) {
                animation = Math.max(0, Math.min(12, animation));
            } else {
                animation = Math.max(24, Math.min(36, animation));
            }
        } else {
            if (previewValue) {
                animation = Math.max(12, animation);
            } else {
                animation = Math.min(24, animation);
            }
        }

        int animation = (int) (LerpUtils.sigmoidZeroOne(this.animation / 36f) * 36);
        if (animation < 3) {
            buttonLoc = GuiTextures.OFF;
        } else if (animation < 13) {
            buttonLoc = GuiTextures.ONE;
        } else if (animation < 23) {
            buttonLoc = GuiTextures.TWO;
        } else if (animation < 33) {
            buttonLoc = GuiTextures.THREE;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(buttonLoc);
        RenderUtils.drawTexturedRect(x + animation, y, 12, 14);
    }

    @Override
    public boolean mouseInput(int mouseX, int mouseY) {
        if (mouseX > x - clickRadius && mouseX < x + xSize + clickRadius && mouseY > y - clickRadius && mouseY < y + ySize + clickRadius) {
            if (Mouse.getEventButton() == 0) {
                if (Mouse.getEventButtonState()) {
                    previewValue = !value;
                } else if (previewValue == !value) {
                    value = !value;
                    toggleCallback.accept(value);
                }
            }
        } else {
            previewValue = value;
        }
        return false;
    }

    @Override
    public boolean keyboardInput() {
        return false;
    }
}
