package de.tudresden.inf.mci.brailleplot.csvparser;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Gregor Harlan, Jens Bornschein Idea and supervising by Jens
 *         Bornschein jens.bornschein@tu-dresden.de Copyright by Technische
 *         Universität Dresden / MCI 2014
 *
 */
public class MetricAxis extends Axis {
    public final double atom;
    public final int atomCount;
    public final List<Double> intervalSteps;

    public MetricAxis(Range axisRange, double size, String title, String unit) {

        // Set the label offsets
        super(-5, 20, -10, 5, 0, title, unit);

        boolean finished = false;
        double interval = 0;
        mRange = new Range(0, 0);
        mRange.setName(axisRange.getName());
        int dimensionExp;
        double dimension;
        double factor;
        do {
            /*
             * Calculate how many tics there can maximally be without violating
             * the minimal distance of grid lines constraint.
             */
            int maxTics = (int) (size / Constants.MIN_GRID_DISTANCE);
            // Calculate which interval (virtual) the tics must minimally have.
            interval = axisRange.distance() / maxTics;
            dimensionExp = 0;
            int direction = interval < 1 ? -1 : 1;
            while (direction * 0.5 * Math.pow(10, dimensionExp) < direction * interval) {
                dimensionExp += direction;
            }
            if (direction == 1) {
                dimensionExp--;
            }
            dimension = Math.pow(10, dimensionExp);
            factor = getFactorForIntervalAndDimension(interval, dimension);
            finished = true;
            interval = factor * dimension * 2;
            mRange.setFrom(((int) (axisRange.getFrom() / interval)) * interval);
            mRange.setTo(((int) (axisRange.getTo() / interval)) * interval);
            if (mRange.getFrom() > axisRange.getFrom()) {
                axisRange.setFrom(mRange.getFrom() - interval);
                finished = false;
            }
            if (mRange.getTo() < axisRange.getTo()) {
                axisRange.setTo(mRange.getTo() + interval);
                finished = false;
            }
        } while (!finished);

        mGridInterval = interval;

        mTicInterval = interval; // TODO set this to 2 * interval if needed, maybe create an option
        mTicRange = new Range(Math.ceil(mRange.getFrom() / mTicInterval) * mTicInterval,
                Math.floor(mRange.getTo() / mTicInterval) * mTicInterval);

        mLabelRange = mTicRange;
        mLabelInterval = mTicInterval * 2;

        mDecimalFormat.setMaximumFractionDigits(Math.max(0, -dimensionExp + 2));

        atom = dimension / 100;
        atomCount = (int) (mRange.distance() / atom + 1);

        intervalSteps = new ArrayList<>();
        calculateIntervalSteps(dimension, factor);
    }

    @Override
    public String formatForAxisLabel(double value) {
        String str = mDecimalFormat.format(value);
        return "-0".equals(str) ? "0" : str;
    }

    @Override
    public String formatForAxisAudioLabel(double value) {
        return formatForAxisLabel(value);
    }

    @Override
    public String formatForSymbolAudioLabel(double value) {
        return formatForAxisLabel(value);
    }

    /**
     * @param dimension
     * @param factor
     */
    private void calculateIntervalSteps(double dimension, double factor) {
        int i = 0;
        if (Math.abs(factor - 2.5) < Constants.EPSILON) {
            intervalSteps.add(i++, 2.5 * dimension);
            intervalSteps.add(i++, dimension);
        } else if (Math.abs(factor - 1) < Constants.EPSILON) {
            intervalSteps.add(i++, dimension);
        }

        intervalSteps.add(i++, 0.5 * dimension);
        intervalSteps.add(i++, 0.1 * dimension);
        intervalSteps.add(i++, 0.05 * dimension);
        intervalSteps.add(i, 0.01 * dimension);
    }

    /**
     * @param interval
     * @param dimension
     * @return the calculated factor
     */
    private double getFactorForIntervalAndDimension(double interval, double dimension) {
        double factor;
        if (interval > dimension) {
            factor = 2.5;
        } else if (interval > 0.5 * dimension) {
            factor = 1;
        } else {
            factor = 0.5;
        }
        return factor;
    }

}
