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

import net.imagej.ops.DefaultOpService;
import net.imagej.ops.Ops;
import net.imagej.ops.special.chain.RAIs;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * Helper Method to generate the meta image used for {@link RidgeDetection}
 * 
 * @author Gabe Selzer
 * @param <T>
 */
public class RidgeMetadataGeneration<T extends RealType<T>> {

	public static <T extends RealType<T>> Img<DoubleType> generateImage(
		RandomAccessibleInterval<T> input, double sigma)
	{
		DefaultOpService opService = new DefaultOpService();
		RandomAccessibleInterval<DoubleType> converted =
			(RandomAccessibleInterval<DoubleType>) opService.run(
				Ops.Convert.Float64.class, input);
		UnaryFunctionOp<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>> copyOp =
			RAIs.function(opService, Ops.Copy.RAI.class, converted);

		Cursor<T> cursor = Views.iterable(input).localizingCursor();
		// create first partial x image, randomAccess
		RandomAccessibleInterval<DoubleType> x = copyOp.calculate(converted);
		int[] derivatives = { 1, 0 };
		opService.filter().derivativeGauss(input, x, sigma, derivatives);
		RandomAccess<DoubleType> xRA = x.randomAccess();

		// create second partial x image, randomAccess
		RandomAccessibleInterval<DoubleType> xx = copyOp.calculate(converted);
		derivatives[0] = 2;
		opService.filter().derivativeGauss(input, xx, sigma, derivatives);
		RandomAccess<DoubleType> xxRA = xx.randomAccess();

		// create first partial x image, first partial y image, randomAccess
		RandomAccessibleInterval<DoubleType> xy = copyOp.calculate(converted);
		derivatives[0] = 1;
		derivatives[1] = 1;
		opService.filter().derivativeGauss(input, xy, sigma, derivatives);
		RandomAccess<DoubleType> xyRA = xy.randomAccess();

		// create first partial y image, cursor, randomAccess
		RandomAccessibleInterval<DoubleType> y = copyOp.calculate(converted);
		derivatives[0] = 0;
		opService.filter().derivativeGauss(input, y, sigma, derivatives);
		RandomAccess<DoubleType> yRA = y.randomAccess();

		// create second partial y image, cursor, randomAccess
		RandomAccessibleInterval<DoubleType> yy = copyOp.calculate(converted);
		derivatives[1] = 2;
		opService.filter().derivativeGauss(input, yy, sigma, derivatives);
		RandomAccess<DoubleType> yyRA = yy.randomAccess();

		// Create an image to hold all of the eigenvalues (slice 0), the x component
		// of the eigenvector (slice 1), the y component of the
		// eigenvector(slice 2), the x component of the line sub-pixel location
		// (slice 3), and the y component of the line sub-pixel location (slice 4).
		long[] metaImageDimensions = { input.dimension(0), input.dimension(1), 5 };
		Img<DoubleType> metaImage = opService.create().img(metaImageDimensions);
		RandomAccess<DoubleType> metaRA = metaImage.randomAccess();

		// loop through the points, fill in potentialPoints with second directional
		// derivative across the line, eigenx with the x component of the normal
		// vector to the line, and eigeny with the y component of that vector.
		while (cursor.hasNext()) {
			cursor.fwd();
			xRA.setPosition(cursor);
			yRA.setPosition(cursor);
			xxRA.setPosition(cursor);
			xyRA.setPosition(cursor);
			yyRA.setPosition(cursor);

			// Get all of the values needed for the point.
			double rx = xRA.get().getRealDouble();
			double ry = yRA.get().getRealDouble();
			double rxx = xxRA.get().getRealDouble();
			double rxy = xyRA.get().getRealDouble();
			double ryy = yyRA.get().getRealDouble();

			// convolve image with 2D partial kernel,
			// make a Hessian using the kernels
			Matrix hessian = new Matrix(input.numDimensions(), input.numDimensions());
			hessian.set(0, 0, xxRA.get().getRealDouble());
			hessian.set(0, 1, xyRA.get().getRealDouble());
			hessian.set(1, 0, xyRA.get().getRealDouble());
			hessian.set(1, 1, yyRA.get().getRealDouble());
			// Jacobian rotation to eliminate rxy
			EigenvalueDecomposition e = hessian.eig();
			Matrix eigenvalues = e.getD();
			Matrix eigenvectors = e.getV();
			// get (nx, ny), i.e. the components of a vector perpendicular to our
			// line, with length of one.
			int index = (Math.abs(eigenvalues.get(0, 0)) > Math.abs(eigenvalues.get(1,
				1))) ? 0 : 1;
			double nx = eigenvectors.get(0, index);
			double ny = eigenvectors.get(1, index);
			double eigenvalue = Math.abs(eigenvalues.get(index, index));

			// obtain (px, py), the point in subpixel space where the first
			// directional derivative vanishes.
			double t = -1 * ((rx * nx) + (ry * ny)) / ((rxx * nx * nx) + (2 * rxy *
				nx * ny) + (ryy * ny * ny));
			double px = t * nx;
			double py = t * ny;

			// so long as the absolute values of px and py are below 0.5, this point
			// is a line point.
			if (Math.abs(px) < 0.5 && Math.abs(py) < 0.5) {
				long[] pos = { cursor.getLongPosition(0), cursor.getLongPosition(1),
					0 };
				// absolute value of gradient
				metaRA.setPosition(pos);
				metaRA.get().setReal(eigenvalue);
				// x component of vector perpendicular to line
				pos[2] = 1;
				metaRA.setPosition(pos);
				metaRA.get().setReal(nx);
				// x component of vector perpendicular to line
				pos[2] = 2;
				metaRA.setPosition(pos);
				metaRA.get().setReal(ny);
				// x component of sub-pixel line location
				pos[2] = 3;
				metaRA.setPosition(pos);
				metaRA.get().setReal(px);
				// y component of sub-pixel line location
				pos[2] = 4;
				metaRA.setPosition(pos);
				metaRA.get().setReal(py);
			}

		}

		return metaImage;

	}

}
