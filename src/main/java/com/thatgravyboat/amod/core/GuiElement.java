package com.thatgravyboat.amod.core;

import net.minecraft.client.gui.Gui;

public abstract class GuiElement extends Gui {

    public abstract void render();

    public abstract boolean mouseInput(int mouseX, int mouseY);

    public abstract boolean keyboardInput();
}
