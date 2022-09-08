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

import net.minecraft.util.Vec3;

/**
 * Represents a line using two points along the line or a segment with endpoints.
 */
public class Line {
	private static final double DOUBLE_EPSILON = 4.94065645841247E-324;
	public Vec3 point1;
	public Vec3 point2;

	public Line(Vec3 first, Vec3 second) {
		point1 = first;
		point2 = second;
	}

	public Vec3 getMidpoint() {
		return new Vec3(
			(point1.xCoord + point2.xCoord) / 2.0,
			(point1.yCoord + point2.yCoord) / 2.0,
			(point1.zCoord + point2.zCoord) / 2.0
		);
	}

	/**
	 * Calculates the intersection line segment between 2 lines
	 * Based on http://paulbourke.net/geometry/pointlineplane/calclineline.cs
	 *
	 * @return The intersection {@link Line} or {@code null} if no solution found
	 */
	public Line getIntersectionLineSegment(Line other) {
		Vec3 p1 = this.point1;
		Vec3 p2 = this.point2;
		Vec3 p3 = other.point1;
		Vec3 p4 = other.point2;
		Vec3 p13 = p1.subtract(p3);
		Vec3 p43 = p4.subtract(p3);

		if (lengthSquared(p43) < DOUBLE_EPSILON) {
			return null;
		}

		Vec3 p21 = p2.subtract(p1);
		if (lengthSquared(p21) < DOUBLE_EPSILON) {
			return null;
		}

		double d1343 = p13.xCoord * p43.xCoord + p13.yCoord * p43.yCoord + p13.zCoord * p43.zCoord;
		double d4321 = p43.xCoord * p21.xCoord + p43.yCoord * p21.yCoord + p43.zCoord * p21.zCoord;
		double d1321 = p13.xCoord * p21.xCoord + p13.yCoord * p21.yCoord + p13.zCoord * p21.zCoord;
		double d4343 = p43.xCoord * p43.xCoord + p43.yCoord * p43.yCoord + p43.zCoord * p43.zCoord;
		double d2121 = p21.xCoord * p21.xCoord + p21.yCoord * p21.yCoord + p21.zCoord * p21.zCoord;

		double denom = d2121 * d4343 - d4321 * d4321;
		if (Math.abs(denom) < DOUBLE_EPSILON) {
			return null;
		}
		double numer = d1343 * d4321 - d1321 * d4343;

		double mua = numer / denom;
		double mub = (d1343 + d4321 * (mua)) / d4343;

		Line resultSegment = new Line(
			new Vec3(
				(float) (p1.xCoord + mua * p21.xCoord),
				(float) (p1.yCoord + mua * p21.yCoord),
				(float) (p1.zCoord + mua * p21.zCoord)
			),
			new Vec3(
				(float) (p3.xCoord + mub * p43.xCoord),
				(float) (p3.yCoord + mub * p43.yCoord),
				(float) (p3.zCoord + mub * p43.zCoord)
			)
		);

		return resultSegment;
	}

	public Line getImmutable() {
		return new Line(point1, point2);
	}

	private static double lengthSquared(Vec3 vec) {
		return vec.dotProduct(vec);
	}

	public String toString() {
		return String.format(
			"point1 = %s, point2 = %s, midpoint = %s",
			point1 == null ? "NULL" : point1.toString(),
			point2 == null ? "NULL" : point2.toString(),
			(point1 == null || point2 == null) ? "NULL" : getMidpoint()
		);
	}
}
