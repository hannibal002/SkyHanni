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

import com.google.gson.annotations.Expose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class Position {
	@Expose
	private int x;
	@Expose
	private int y;
	@Expose
	private boolean centerX;
	@Expose
	private boolean centerY;

	private boolean clicked = false;
	public String internalName = null;

	public Position(int x, int y) {
		this(x, y, false, false);
	}

	public Position(int x, int y, boolean centerX, boolean centerY) {
		this.x = x;
		this.y = y;
		this.centerX = centerX;
		this.centerY = centerY;
	}

	public void set(Position other) {
		this.x = other.x;
		this.y = other.y;
		this.centerX = other.centerX;
		this.centerY = other.centerY;
	}

	public Position clone() {
		Position position = new Position(x, y, centerX, centerY);
		position.internalName = internalName;
		return position;
	}

//	public boolean isCenterX() {
//		return ;
//	}

	public int getRawX() {
		return x;
	}

	public int getRawY() {
		return y;
	}

	public void setClicked(boolean state) {
		this.clicked = state;
	}
	public boolean getClicked() {
		return clicked;
	}

	public int getAbsX0(int objWidth) {
		int width = new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth();

		int ret = x;
		if (x < 0) {
			ret = width + x - objWidth;
		}

		if (ret < 0) ret = 0;
		if (ret > width - objWidth) ret = width - objWidth;

		return ret;
	}

	public int getAbsY0(int objHeight) {
		int height = new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight();

		int ret = y;
		if (y < 0) {
			ret = height + y - objHeight;
		}

		if (ret < 0) ret = 0;
		if (ret > height - objHeight) ret = height - objHeight;

		return ret;
	}

	public int moveX(int deltaX) {
		int screenWidth = new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth();
		boolean wasPositiveX = this.x >= 0;
		this.x += deltaX;

		if (wasPositiveX) {
			if (this.x < 0) {
				deltaX += -this.x;
				this.x = 0;
			}
			if (this.x > screenWidth) {
				deltaX += screenWidth - this.x;
				this.x = screenWidth;
			}
		} else {
			if (this.x + 1 > 0) {
				deltaX += -1 - this.x;
				this.x = -1;
			}
			if (this.x + screenWidth < 0) {
				deltaX += -screenWidth - this.x;
				this.x = -screenWidth;
			}
		}

		return deltaX;
	}

	public int moveY(int deltaY) {
		int screenHeight = new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight();
		boolean wasPositiveY = this.y >= 0;
		this.y += deltaY;

		if (wasPositiveY) {
			if (this.y < 0) {
				deltaY += -this.y;
				this.y = 0;
			}
			if (this.y > screenHeight) {
				deltaY += screenHeight - this.y;
				this.y = screenHeight;
			}
		} else {
			if (this.y + 1 > -0) {
				deltaY += -1 - this.y;
				this.y = -1;
			}
			if (this.y + screenHeight < 0) {
				deltaY += -screenHeight - this.y;
				this.y = -screenHeight;
			}
		}

		return deltaY;
	}
}
