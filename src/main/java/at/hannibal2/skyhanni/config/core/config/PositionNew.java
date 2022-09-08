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
import net.minecraft.client.gui.ScaledResolution;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PositionNew {
	public enum Anchor {
		MIN(0, 0, 0),
		MID(0.5f, -0.5f, 0),
		MAX(1f, -1f, 0),
		GUI_MIN(0.5f, -1f, -0.5f),
		GUI_MAX(0.5f, 0, 0.5f);

		float screenMult;
		float elementMult;
		float guiMult;

		Anchor(float screenMult, float elementMult, float guiMult) {
			this.screenMult = screenMult;
			this.elementMult = elementMult;
			this.guiMult = guiMult;
		}
	}

	@Expose
	private int x = 0;
	@Expose
	private int y = 0;
	@Expose
	private float scaleX = 1;
	@Expose
	private float scaleY = 1;

	@Expose
	private Anchor anchorX = Anchor.MIN;
	@Expose
	private Anchor anchorY = Anchor.MIN;

	@Expose
	private boolean pinned = false;
	@Expose
	private boolean allowPinToggle = true;
	@Expose
	private boolean allowResize = true;

	public PositionNew(
		int x,
		int y,
		int scaleX,
		int scaleY,
		Anchor anchorX,
		Anchor anchorY,
		boolean pinned,
		boolean allowPinToggle,
		boolean allowResize
	) {
		this.x = x;
		this.y = y;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.anchorX = anchorX;
		this.anchorY = anchorY;
		this.pinned = pinned;
		this.allowPinToggle = allowPinToggle;
		this.allowResize = allowResize;
	}

	protected PositionNew() {}

	public int moveX(ScaledResolution scaledResolution, int deltaX, int sizeX) {
		int originalX = resolveX(scaledResolution, sizeX);
		AtomicInteger atomicInteger = new AtomicInteger(x + deltaX);
		AtomicReference<Anchor> atomicReference = new AtomicReference<>(anchorX);
		move(
			atomicInteger,
			atomicReference,
			anchorY,
			(int) Math.ceil(sizeX * scaleX),
			scaledResolution.getScaledWidth(),
			176
		);
		x = atomicInteger.get();
		anchorX = atomicReference.get();
		return resolveX(scaledResolution, sizeX) - originalX;
	}

	public int moveY(ScaledResolution scaledResolution, int deltaY, int sizeY) {
		int originalY = resolveY(scaledResolution, sizeY);
		AtomicInteger atomicInteger = new AtomicInteger(y + deltaY);
		AtomicReference<Anchor> atomicReference = new AtomicReference<>(anchorY);
		move(
			atomicInteger,
			atomicReference,
			anchorY,
			(int) Math.ceil(sizeY * scaleY),
			scaledResolution.getScaledHeight(),
			166
		);
		y = atomicInteger.get();
		anchorY = atomicReference.get();
		return resolveY(scaledResolution, sizeY) - originalY;
	}

	private void move(
		AtomicInteger coord,
		AtomicReference<Anchor> anchor,
		Anchor oppositeAnchor,
		int elementSize,
		int screenSize,
		int guiSize
	) {
		int centerCoord = resolve(coord.get(), anchor.get(), elementSize, screenSize, guiSize) + elementSize / 2;

		if (centerCoord < screenSize / 2 - guiSize / 2) {
			if (pinned && centerCoord > screenSize / 4 - guiSize / 4 && oppositeAnchor == Anchor.MID) {
				anchor.set(Anchor.GUI_MIN);
			} else {
				anchor.set(Anchor.MIN);
			}
		} else if (centerCoord > screenSize / 2 + guiSize / 2) {
			if (pinned && centerCoord < screenSize - (screenSize / 4 - guiSize / 4) && oppositeAnchor == Anchor.MID) {
				anchor.set(Anchor.GUI_MAX);
			} else {
				anchor.set(Anchor.MAX);
			}
		} else {
			anchor.set(Anchor.MID);
		}

		if (centerCoord - elementSize / 2 < 0) centerCoord = elementSize / 2;
		if (centerCoord + elementSize / 2 > screenSize) centerCoord = screenSize - elementSize / 2;

		Anchor newAnchor = anchor.get();
		coord.set(Math.round(
			centerCoord - (elementSize * (newAnchor.elementMult + 0.5f)) - screenSize * newAnchor.screenMult -
				guiSize * newAnchor.guiMult));
	}

	public int resolveX(ScaledResolution scaledResolution, int sizeX) {
		return resolve(x, anchorX, (int) Math.ceil(sizeX * scaleX), scaledResolution.getScaledWidth(), 176);
	}

	public int resolveY(ScaledResolution scaledResolution, int sizeY) {
		return resolve(y, anchorY, (int) Math.ceil(sizeY * scaleY), scaledResolution.getScaledHeight(), 166);
	}

	private int resolve(int coord, Anchor anchor, int elementSize, int screenSize, int guiSize) {
		return Math.round(
			screenSize * anchor.screenMult + elementSize * anchor.elementMult + guiSize * anchor.guiMult + coord);
	}

	public void setScaleX(float scaleX) {
		if (allowResize) {
			this.scaleX = scaleX;
		}
	}

	public void setScaleY(float scaleY) {
		if (allowResize) {
			this.scaleY = scaleY;
		}
	}

	public float getScaleX() {
		return scaleX;
	}

	public float getScaleY() {
		return scaleY;
	}

	public void setPinned(boolean pinned) {
		if (allowPinToggle) {
			this.pinned = pinned;
		}
	}

	public boolean isPinned() {
		return pinned;
	}

	public Anchor getAnchorX() {
		return anchorX;
	}

	public Anchor getAnchorY() {
		return anchorY;
	}
}
