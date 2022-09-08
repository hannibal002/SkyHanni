/*
 * Copyright (C) 2022 NotEnoughUpdates contributors
 *
 * This file is part of NotEnoughUpdates.
 *
 * NotEnoughUpdates is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * NotEnoughUpdates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
 */

package at.hannibal2.skyhanni.config.core.config.gui;

import at.hannibal2.skyhanni.config.Features;
import at.hannibal2.skyhanni.config.core.GuiElementBoolean;
import at.hannibal2.skyhanni.config.core.config.struct.ConfigProcessor;

public class GuiOptionEditorBoolean extends GuiOptionEditor {

    private final GuiElementBoolean bool;
    private final Features config;
    private final String runnableId;

    public GuiOptionEditorBoolean(
            ConfigProcessor.ProcessedOption option,
            String runnableId,
            Features config
    ) {
        super(option);
        this.config = config;
        this.runnableId = runnableId;
        bool = new GuiElementBoolean(0, 0, (boolean) option.get(), 10, (value) -> onUpdate(option, value));
    }

    @Override
    public void render(int x, int y, int width) {
        super.render(x, y, width);
        int height = getHeight();

        bool.x = x + width / 6 - 24;
        bool.y = y + height - 7 - 14;
        bool.render();
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        int height = getHeight();
        bool.x = x + width / 6 - 24;
        bool.y = y + height - 7 - 14;
        return bool.mouseInput(mouseX, mouseY);
    }

    @Override
    public boolean keyboardInput() {
        return false;
    }

    private void onUpdate(ConfigProcessor.ProcessedOption option, boolean value) {
        if (option.set(value)) {
            config.executeRunnable(runnableId);
        }
    }
}
