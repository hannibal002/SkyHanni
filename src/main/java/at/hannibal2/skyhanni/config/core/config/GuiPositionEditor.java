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

package at.hannibal2.skyhanni.config.core.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

import java.io.IOException;

public class GuiPositionEditor extends GuiScreen {
	public PositionNew position = new PositionNew();

	public int clickedX;
	public int clickedY;

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawDefaultBackground();

		ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
		int width = scaledResolution.getScaledWidth();
		int height = scaledResolution.getScaledHeight();

		int x = position.resolveX(scaledResolution, 200);
		int y = position.resolveY(scaledResolution, 100);

		int centerWidth = 176;
		int centerHeight = 166;

		float centerWF = centerWidth / 2f / (float) width;
		float centerHF = centerHeight / 2f / (float) height;

		float left = 0;
		float top = 0;
		float right = 0;
		float bottom = 0;

		switch (position.getAnchorX()) {
			case MIN: {
				left = 0;
				right = 0.5f - centerWF;
				break;
			}
			case MID: {
				left = 0.5f - centerWF;
				right = 0.5f + centerWF;
				break;
			}
			case MAX: {
				left = 0.5f + centerWF;
				right = 1;
				break;
			}
		}
		switch (position.getAnchorY()) {
			case MIN: {
				top = 0;
				bottom = 0.5f - centerHF;
				break;
			}
			case MID: {
				top = 0.5f - centerHF;
				bottom = 0.5f + centerHF;
				break;
			}
			case MAX: {
				top = 0.5f + centerHF;
				bottom = 1;
				break;
			}
		}

		Gui.drawRect(
			(int) (left * width),
			(int) (top * height),
			(int) (right * width),
			(int) (bottom * height),
			0x40404040
		);
		Gui.drawRect(x, y, x + 200, y + 100, 0x80404040);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
		int x = position.resolveX(scaledResolution, 200);
		int y = position.resolveY(scaledResolution, 100);

		if (mouseX > x && mouseX < x + 200 &&
			mouseY > y && mouseY < y + 100) {
			clickedX = mouseX;
			clickedY = mouseY;
		} else {
			clickedX = -1;
			clickedY = -1;
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		clickedX = -1;
		clickedY = -1;
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		if (clickedX >= 0 && clickedY >= 0) {
			int deltaX = mouseX - clickedX;
			int deltaY = mouseY - clickedY;

			ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());

			deltaX = position.moveX(scaledResolution, deltaX, 200);
			deltaY = position.moveY(scaledResolution, deltaY, 100);

			clickedX += deltaX;
			clickedY += deltaY;
		}
	}
}
