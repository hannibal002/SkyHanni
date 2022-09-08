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

package at.hannibal2.skyhanni.config.core.util;

import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;

public class Vec3Comparable extends Vec3 implements Comparable<Vec3Comparable> {
	public static final Vec3Comparable NULL_VECTOR = new Vec3Comparable(0.0, 0.0, 0.0);

	public Vec3Comparable(double x, double y, double z) {
		super(x, y, z);
	}

	public Vec3Comparable(Vec3i sourceVec) {
		super(sourceVec);
	}

	public Vec3Comparable(Vec3 source) {
		super(source.xCoord, source.yCoord, source.zCoord);
	}

	public Vec3Comparable(BlockPos source) {

		super(source.getX(), source.getY(), source.getZ());
	}

	public Vec3Comparable(Vec3Comparable source) {
		super(source.xCoord, source.yCoord, source.zCoord);
	}

	@Override
	public Vec3Comparable subtractReverse(Vec3 vec) {
		return new Vec3Comparable(super.subtractReverse(vec));
	}

	@Override
	public Vec3Comparable normalize() {
		return new Vec3Comparable(super.normalize());
	}

	@Override
	public Vec3Comparable crossProduct(Vec3 vec) {
		return new Vec3Comparable(super.crossProduct(vec));
	}

	@Override
	public Vec3Comparable subtract(Vec3 vec) {
		return new Vec3Comparable(super.subtract(vec));
	}

	@Override
	public Vec3Comparable subtract(double x, double y, double z) {
		return new Vec3Comparable(super.subtract(x, y, z));
	}

	@Override
	public Vec3Comparable add(Vec3 other) {
		return new Vec3Comparable(super.add(other));
	}

	@Override
	public Vec3Comparable addVector(double x, double y, double z) {
		return new Vec3Comparable(super.addVector(x, y, z));
	}

	@Override
	public Vec3Comparable getIntermediateWithXValue(Vec3 vec, double x) {
		return new Vec3Comparable(super.getIntermediateWithXValue(vec, x));
	}

	@Override
	public Vec3Comparable getIntermediateWithYValue(Vec3 vec, double y) {
		return new Vec3Comparable(super.getIntermediateWithYValue(vec, y));
	}

	@Override
	public Vec3Comparable getIntermediateWithZValue(Vec3 vec, double z) {
		return new Vec3Comparable(super.getIntermediateWithZValue(vec, z));
	}

	@Override
	public Vec3Comparable rotatePitch(float pitch) {
		return new Vec3Comparable(super.rotatePitch(pitch));
	}

	@Override
	public Vec3Comparable rotateYaw(float yaw) {
		return new Vec3Comparable(super.rotateYaw(yaw));
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (!(other instanceof Vec3Comparable)) {
			return false;
		} else {
			Vec3Comparable vec3c = (Vec3Comparable) other;
			return this.xCoord == vec3c.xCoord && this.yCoord == vec3c.yCoord && this.zCoord == vec3c.zCoord;
		}
	}

	@Override
	public int hashCode() {
		long bits = 1L;
		bits = 31L * bits + doubleToLongBits(xCoord);
		bits = 31L * bits + doubleToLongBits(yCoord);
		bits = 31L * bits + doubleToLongBits(zCoord);
		return (int) (bits ^ (bits >> 32));
	}

	public int compareTo(Vec3Comparable other) {
		return this.yCoord == other.yCoord ?
			(this.zCoord == other.zCoord ?
				(int) (this.xCoord - other.xCoord)
				: (int) (this.zCoord - other.zCoord))
			: (int) (this.yCoord - other.yCoord);
	}

	public boolean signumEquals(Vec3 other) {
		return Math.signum(xCoord) == Math.signum(other.xCoord) &&
			Math.signum(yCoord) == Math.signum(other.yCoord) &&
			Math.signum(zCoord) == Math.signum(other.zCoord);
	}

	private static long doubleToLongBits(double d) {
		return d == 0.0 ? 0L : Double.doubleToLongBits(d);
	}
}
