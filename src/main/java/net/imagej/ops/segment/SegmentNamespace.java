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

package net.imagej.ops.segment;

import java.util.List;

import net.imagej.ops.AbstractNamespace;
import net.imagej.ops.Namespace;
import net.imagej.ops.OpMethod;
import net.imagej.ops.segment.hough.HoughCircle;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

import org.scijava.app.StatusService;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

/**
 * The segment namespace contains segmentation operations.
 * 
 * @author Gabe Selzer
 */
@Plugin(type = Namespace.class)
public class SegmentNamespace extends AbstractNamespace {

	// -- Segmentation namespace ops --

	@OpMethod(op = net.imagej.ops.segment.hough.HoughCircleDetectorDogOp.class)
	public <T extends RealType<T> & NativeType<T>> List<HoughCircle>
		houghCircleDetectorDog(final RandomAccessibleInterval<T> in,
			final ThreadService ts, final double circleThickness,
			final double minRadius, final double stepRadius, final double sigma)
	{
		@SuppressWarnings("unchecked")
		final List<HoughCircle> result = (List<HoughCircle>) ops().run(
			net.imagej.ops.Ops.Segment.HoughCircleDetect.class, in, ts,
			circleThickness, minRadius, stepRadius, sigma);
		return result;
	}

	@OpMethod(op = net.imagej.ops.segment.hough.HoughCircleDetectorDogOp.class)
	public <T extends RealType<T> & NativeType<T>> List<HoughCircle>
		houghCircleDetectorDog(final RandomAccessibleInterval<T> in,
			final ThreadService ts, final double circleThickness,
			final double minRadius, final double stepRadius, final double sigma,
			final double sensitivity)
	{
		@SuppressWarnings("unchecked")
		final List<HoughCircle> result = (List<HoughCircle>) ops().run(
			net.imagej.ops.Ops.Segment.HoughCircleDetect.class, in, ts,
			circleThickness, minRadius, stepRadius, sigma, sensitivity);
		return result;
	}

	@OpMethod(
		op = net.imagej.ops.segment.hough.HoughCircleDetectorLocalExtremaOp.class)
	public <T extends RealType<T> & NativeType<T>> List<HoughCircle>
		houghCircleDetectorLocalExtrema(final RandomAccessibleInterval<T> in,
			final ThreadService ts, final double circleThickness,
			final double minRadius, final double stepRadius)
	{
		@SuppressWarnings("unchecked")
		final List<HoughCircle> result = (List<HoughCircle>) ops().run(
			net.imagej.ops.Ops.Segment.HoughCircleDetect.class, in, ts,
			circleThickness, minRadius, stepRadius);
		return result;
	}

	@OpMethod(
		op = net.imagej.ops.segment.hough.HoughCircleDetectorLocalExtremaOp.class)
	public <T extends RealType<T> & NativeType<T>> Img<DoubleType>
		houghCircleDetectorLocalExtrema(final RandomAccessibleInterval<T> in,
			final ThreadService ts, final double circleThickness,
			final double minRadius, final double stepRadius, final double sensitivity)
	{
		@SuppressWarnings("unchecked")
		final Img<DoubleType> result = (Img<DoubleType>) ops().run(
			net.imagej.ops.Ops.Segment.HoughCircleDetect.class, in, ts,
			circleThickness, minRadius, stepRadius, sensitivity);
		return result;
	}

	@OpMethod(op = net.imagej.ops.segment.hough.HoughTransformOpNoWeights.class)
	public <T extends BooleanType<T>> Img<DoubleType> houghCircleTransform(
		final Img<DoubleType> out, final IterableInterval<T> in,
		final StatusService statusService, final double minRadius,
		final double maxRadius, final double stepRadius)
	{
		@SuppressWarnings("unchecked")
		final Img<DoubleType> result = (Img<DoubleType>) ops().run(
			net.imagej.ops.Ops.Segment.HoughCircleTransform.class, out, in,
			statusService, minRadius, maxRadius, stepRadius);
		return result;
	}

	@OpMethod(op = net.imagej.ops.segment.hough.HoughTransformOpNoWeights.class)
	public <T extends BooleanType<T>> Img<DoubleType> houghCircleTransform(
		final IterableInterval<T> in, final StatusService statusService,
		final double minRadius, final double maxRadius, final double stepRadius)
	{
		@SuppressWarnings("unchecked")
		final Img<DoubleType> result = (Img<DoubleType>) ops().run(
			net.imagej.ops.Ops.Segment.HoughCircleTransform.class, in, statusService,
			minRadius, maxRadius, stepRadius);
		return result;
	}

	@OpMethod(op = net.imagej.ops.segment.hough.HoughTransformOpWeights.class)
	public <T extends BooleanType<T>, R extends RealType<R>> Img<DoubleType>
		houghCircleTransform(final Img<DoubleType> out,
			final IterableInterval<T> in, final StatusService statusService,
			final double minRadius, final double maxRadius, final double stepRadius,
			final RandomAccessible<R> weights)
	{
		@SuppressWarnings("unchecked")
		final Img<DoubleType> result = (Img<DoubleType>) ops().run(
			net.imagej.ops.Ops.Segment.HoughCircleTransform.class, out, in,
			statusService, minRadius, maxRadius, stepRadius, weights);
		return result;
	}

	@OpMethod(op = net.imagej.ops.segment.hough.HoughTransformOpWeights.class)
	public <T extends BooleanType<T>, R extends RealType<R>> Img<DoubleType>
		houghCircleTransform(final IterableInterval<T> in,
			final StatusService statusService, final double minRadius,
			final double maxRadius, final double stepRadius,
			final RandomAccessible<R> weights)
	{
		@SuppressWarnings("unchecked")
		final Img<DoubleType> result = (Img<DoubleType>) ops().run(
			net.imagej.ops.Ops.Segment.HoughCircleTransform.class, in, statusService,
			minRadius, maxRadius, stepRadius, weights);
		return result;
	}

	@Override
	public String getName() {
		return "segment";
	}

}
