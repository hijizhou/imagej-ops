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
import java.util.List;

import net.imagej.ops.Contingent;
import net.imagej.ops.Op;
import net.imagej.ops.Ops;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.roi.geom.real.DefaultPolyline;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Op.class)
public class EdgeDetection<T extends RealType<T>> extends
	AbstractBinaryFunctionOp<RandomAccessibleInterval<T>, List<DefaultPolyline>, List<DefaultPolyline>>
	implements Contingent
{

	@Parameter
	double sigma;

	@Override
	public List<DefaultPolyline> calculate(RandomAccessibleInterval<T> input,
		List<DefaultPolyline> lines)
	{
		List<DefaultPolyline> edges = new ArrayList<DefaultPolyline>();
		double edgeLimit = 2.5 * sigma;

		int[] derivatives = { 1, 1 };
		RandomAccessibleInterval<DoubleType> derivative =
			(RandomAccessibleInterval<DoubleType>) ops().run(Ops.Copy.RAI.class,
				input);
		RandomAccess<DoubleType> derivativeRA = derivative.randomAccess();

		ops().filter().derivativeGauss(input, derivative, sigma, derivatives);

		// loop through each line,find the edge if within the limit, otherwise
		// extrapolate for a nice output.
		for (int i = 0; i < lines.size(); i++) {
			DefaultPolyline currentLine = lines.get(i);

			for (int p = 0; p < currentLine.numVertices(); p++) {
				// find the direction tangent to the line
				double[] currentCoords = currentLine.vertex(p);
				if (p < currentLine.numVertices() - 1) {
					double[] nextCoords = currentLine.vertex(p);
					double distx = nextCoords[0] - currentCoords[0];
					double disty = nextCoords[1] - currentCoords[1];
					int[] RADirection = RidgeDetectionUtils.getOctantCoords(
						RidgeDetectionUtils.getOctant(distx, disty));
					
					int[] dist = {0 , 0};
					double distance = Math.sqrt(dist[0] * dist[0] + dist[1] * dist[1]);
					boolean foundEdge = false;
					while(distance < edgeLimit) {
						//left edge - TODO
						
						//right edge TODO
					}
				}
			}

		}

		return edges;
	}

	@Override
	public boolean conforms() {
		return in().numDimensions() == 2;
	}

}
