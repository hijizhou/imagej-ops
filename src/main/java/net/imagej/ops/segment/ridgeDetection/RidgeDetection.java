/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2014 - 2017 Board of Regents of the University of
 * Wisconsin-Madison, University of Konstanz and Brian Northan.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.ops.segment.ridgeDetection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.imagej.ops.Contingent;
import net.imagej.ops.Op;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory.Boundary;
import net.imglib2.roi.geom.real.DefaultPolyline;
import net.imglib2.roi.geom.real.Polyline;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * Performs the Ridge Detection algorithm on a 2-Dimensional, gray-scale image.
 * 
 * @author Gabe Selzer
 */
@Plugin(type = Op.class)
public class RidgeDetection<T extends RealType<T>> extends
	AbstractUnaryFunctionOp<RandomAccessibleInterval<T>, List<DefaultPolyline>>
	implements Contingent
{

	@Parameter
	double width;
	/**
	 * This method recursively generates the list of points to be created in a
	 * line.
	 * 
	 * @param metaRA - The {@link RandomAccess} of the meta image.
	 * @param points - The {@link List} of points on the line.
	 * @param octant - The octant of the vector parallel to the line as of the
	 *          last point.
	 * @param lastnx - The nx of the last point.
	 * @param lastny - The ny of the last point.
	 * @param lastpx - The px of the last point.
	 * @param lastpy - The py of the last point.
	 */
	private void getNextPoint(RandomAccess<DoubleType> metaRA, List<Point> points,
		int octant, double lastnx, double lastny, double lastpx, double lastpy)
	{
		Point currentPos = new Point(metaRA);
		// variables for the best line point of the three.
		Point salientPoint = new Point(metaRA);
		double salientnx = 0;
		double salientny = 0;
		double salientpx = 0;
		double salientpy = 0;
		double bestSalience = Double.MAX_VALUE;
		boolean lastPointInLine = true;
		// check the three possible points that could continue the line, starting at
		// the octant before and ending at the one after
		double lastAngle = RidgeDetectionUtils.getAngle(lastnx, lastny);
		for (int i = -1; i <= 1; i++) {
			int[] modifier = RidgeDetectionUtils.getOctantCoords(octant + i);
			metaRA.move(modifier[0], 0);
			metaRA.move(modifier[1], 1);
			// make sure that we only do the calculations if there is a line point
			// there.
			metaRA.setPosition(0, 2);
			if (metaRA.get().get() != 0) {
				lastPointInLine = false;
				// third dimension of image is, from 0 to 4, magnitude, nx, ny, px, py.
				// We
				// are obtaining the last four for calculations.
				metaRA.setPosition(1, 2);
				double nx = metaRA.get().get();
				metaRA.fwd(2);
				double ny = metaRA.get().get();
				metaRA.fwd(2);
				double px = metaRA.get().get();
				metaRA.fwd(2);
				double py = metaRA.get().get();
				double currentAngle = RidgeDetectionUtils.getAngle(nx, ny);
				double subpixelDiff = Math.sqrt(Math.pow(px - lastpx, 2) + Math.pow(py -
					lastpy, 2));
				double angleDiff = Math.abs(currentAngle - lastAngle);
				// A salient line point will have the smallest combination of these
				// numbers relative to the other two
				if (subpixelDiff + angleDiff < bestSalience) {
					salientPoint = RidgeDetectionUtils.get2DPoint(metaRA);
					salientnx = nx;
					salientny = ny;
					salientpx = px;
					salientpy = py;
					bestSalience = subpixelDiff + angleDiff;
				}
			}

			// reset our randomAccess for the next check
			metaRA.setPosition(currentPos);
		}

		// set the current pixel to 0 in the first slice of eigenRA!
		metaRA.get().setReal(0);

		// find the next line point as long as there is one to find
		if (!lastPointInLine) {
			// take the most salient point
			metaRA.setPosition(salientPoint);
			points.add(RidgeDetectionUtils.get2DPoint(metaRA));
			// perform the operation again on the new end of the line being formed.
			getNextPoint(metaRA, points, RidgeDetectionUtils.getOctant(salientnx, salientny), salientnx,
				salientny, salientpx, salientpy);
		}
	}

	public List<DefaultPolyline> calculate(RandomAccessibleInterval<T> input) {

		double sigma = (width / Math.sqrt(3));

		// Create an image to hold all of the eigenvalues (slice 0), the x component
		// of the eigenvector (slice 1), the y component of the
		// eigenvector(slice 2), the x component of the line sub-pixel location
		// (slice 3), and the y component of the line sub-pixel location (slice 4).
		
		Img<DoubleType> metaImage = RidgeMetadataGeneration.generateImage(input, sigma);
		RandomAccess<DoubleType> metaRA = metaImage.randomAccess();

		// build the points of potentialPoints into polylines
		Img<T> absoluteValues = (Img<T>) Views.hyperSlice(metaImage, 2, 0);
		RandomAccess<T> absRA = absoluteValues.randomAccess();

		// create the output polyline list.
		List<DefaultPolyline> lines = new ArrayList<DefaultPolyline>();
		// start at the point of greatest maximum absolute value
		input.max(absRA);
		while (absRA.getLongPosition(0) != 0 && absRA.getLongPosition(1) != 0) {
			// create the List of points that will be used to make the polyline
			List<Point> points = new ArrayList<Point>();
			points.add(new Point(absRA));
			// get all of the necessary metadata from the image.
			long[] eigenvectorPos = { absRA.getLongPosition(0), absRA.getLongPosition(
				0), 1 };
			metaRA.setPosition(eigenvectorPos);
			double eigenx = metaRA.get().getRealDouble();
			eigenvectorPos[2] = 2;
			metaRA.setPosition(eigenvectorPos);
			double eigeny = metaRA.get().getRealDouble();
			eigenvectorPos[2] = 3;
			metaRA.setPosition(eigenvectorPos);
			double px = metaRA.get().getRealDouble();
			eigenvectorPos[2] = 4;
			metaRA.setPosition(eigenvectorPos);
			double py = metaRA.get().getRealDouble();

			// determine octant of perpendicular vector
			int octant = RidgeDetectionUtils.getOctant(eigenx, eigeny);

			// go in the direction to the right of the perpendicular value
			int dir1 = octant > 2 ? octant - 2 : octant + 6;
			getNextPoint(metaRA, points, dir1, eigenx, eigeny, px, py);

			// flip the array list around so that we get one cohesive line TODO
			Collections.reverse(points);

			// go in the opposite direction as before.
			int dir2 = octant < 7 ? octant + 2 : octant - 6;
			getNextPoint(metaRA, points, dir2, eigenx, eigeny, px, py);

			// set the value to 0 so that it is not reused.
			absRA.get().setReal(0);

			// turn the list of points into a polyline, add to output list.
			DefaultPolyline pline = new DefaultPolyline(points);
			lines.add(pline);
		}

		return lines;
	}

	@Override
	public boolean conforms() {
		return in().numDimensions() == 2;
	}

}
