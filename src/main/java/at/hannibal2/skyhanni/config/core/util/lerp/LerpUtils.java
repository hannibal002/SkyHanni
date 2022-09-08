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

package at.hannibal2.skyhanni.config.core.util.lerp;

public class LerpUtils {
	public static float clampZeroOne(float f) {
		return Math.max(0, Math.min(1, f));
	}

	public static float sigmoid(float val) {
		return (float) (1 / (1 + Math.exp(-val)));
	}

	private static final float sigmoidStr = 8;
	private static final float sigmoidA = -1 / (sigmoid(-0.5f * sigmoidStr) - sigmoid(0.5f * sigmoidStr));
	private static final float sigmoidB = sigmoidA * sigmoid(-0.5f * sigmoidStr);

	public static float sigmoidZeroOne(float f) {
		f = clampZeroOne(f);
		return sigmoidA * sigmoid(sigmoidStr * (f - 0.5f)) - sigmoidB;
	}

	public static float lerp(float a, float b, float amount) {
		return b + (a - b) * clampZeroOne(amount);
	}
}
