package net.imagej.ops.segment.hough;

import org.scijava.ItemIO;
import org.scijava.Priority;
import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.Contingent;
import net.imagej.ops.special.hybrid.AbstractUnaryHybridCF;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.BooleanType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Hough transform for binary images.
 * 
 * @author Jean-Yves Tinevez
 *
 * @param <T>
 *            the type of source image. Must extend boolean type.
 */
@Plugin( type = HoughCircleTransformOp.class, priority = Priority.HIGH )
public class HoughTransformOpNoWeights< T extends BooleanType< T > >
		extends AbstractUnaryHybridCF< IterableInterval< T >, Img< DoubleType > >
		implements Contingent
{

	@Parameter
	protected StatusService statusService;

	@Parameter( label = "Min circle radius", description = "Minimal radius, in pixel units, for the transform.", min = "1", type = ItemIO.INPUT )
	protected long minRadius = 1;

	@Parameter( label = "Max circle radius", description = "Maximal radius, in pixel units, for the transform.", min = "1", type = ItemIO.INPUT )
	protected long maxRadius = 50;

	@Parameter( label = "Step radius", description = "Radius step, in pixel units, for the transform.", min = "1", type = ItemIO.INPUT, required = false )
	protected long stepRadius = 1;

	@Override
	public boolean conforms() {
		return in().numDimensions() == 2;
	}

	@Override
	public Img< DoubleType > createOutput( final IterableInterval< T > input )
	{
		maxRadius = Math.max( minRadius, maxRadius );
		minRadius = Math.min( minRadius, maxRadius );
		final long nRadiuses = ( maxRadius - minRadius ) / stepRadius + 1;

		/*
		 * Voting image.
		 */

		final int numDimensions = input.numDimensions();
		final long[] dims = new long[ numDimensions + 1 ];
		for ( int d = 0; d < numDimensions; d++ )
			dims[ d ] = input.dimension( d );
		dims[ numDimensions ] = nRadiuses;
		final Dimensions dimensions = FinalDimensions.wrap( dims );
		final ImgFactory< DoubleType > factory = ops().create().imgFactory( dimensions );
		final Img< DoubleType > votes = factory.create( dimensions, new DoubleType() );
		return votes;
	}

	@Override
	public void compute( final IterableInterval< T > input, final Img< DoubleType > output )
	{
		final long maxR = Math.max( minRadius, maxRadius );
		final long minR = Math.min( minRadius, maxRadius );
		maxRadius = maxR;
		minRadius = minR;
		final long nRadiuses = ( maxRadius - minRadius ) / stepRadius + 1;

		/*
		 * Hough transform.
		 */

		final double sum = ops().stats().sum( input ).getRealDouble();
		int progress = 0;

		final Cursor< T > cursor = input.localizingCursor();
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			if ( !cursor.get().get() )
				continue;

			for ( int i = 0; i < nRadiuses; i++ )
			{
				final IntervalView< DoubleType > slice = Views.hyperSlice( output, input.numDimensions(), i );
				final long r = minRadius + i * stepRadius;
				MidPointAlgorithm.inc( Views.extendZero( slice ), cursor, r );
			}

			statusService.showProgress( ++progress, ( int ) sum );
		}
	}
}
