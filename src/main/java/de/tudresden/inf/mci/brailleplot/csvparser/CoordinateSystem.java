package de.tudresden.inf.mci.brailleplot.csvparser;

import java.util.List;


/**
 *
 * @author Gregor Harlan, Jens Bornschein Idea and supervising by Jens
 *         Bornschein jens.bornschein@tu-dresden.de Copyright by Technische
 *         Universität Dresden / MCI 2014
 *
 */
public class CoordinateSystem {

    public final Axis mXAxis;
    public final Axis mYAxis;

    public final boolean mPi;

    /** Origin of the real coordinate system (left upper corner). */
    private final Point mOrigin;

    /** Size of the drawing area excluding margins. */
    private final Point mSize;

    public static final int CONSTANT = 3;

    /**
     * Constructor for a coordinate system with a nominal mX axis. TODO replace
     * by a factory in order to avoid code duplication
     *
     * @param xCategories List(String)
     * @param yRange Range
     * @param size Point
     * @param diagramContentMargin List(Integer)
     * @param xUnit String
     * @param yUnit String
     */
    public CoordinateSystem(final List<String> xCategories, final Range yRange, final Point size, final List<Integer> diagramContentMargin, final String xUnit, final String yUnit) {
        mOrigin = new Point(diagramContentMargin.get(CONSTANT), diagramContentMargin.get(0));

        this.mSize = new Point(size);
        this.mSize.setX(this.mSize.getX() - (diagramContentMargin.get(1) + diagramContentMargin.get(CONSTANT)));
        this.mSize.setY(this.mSize.getY() - (diagramContentMargin.get(0) + diagramContentMargin.get(2)));
        // this.mSize.mX = Math.min(this.mSize.mX, this.mSize.mY);
        // this.mSize.mY = this.mSize.mX;

        mXAxis = new NominalAxis(xCategories, this.mSize.getX(), xUnit);
        mYAxis = new MetricAxis(yRange, this.mSize.getY(), yRange.getName(), yUnit);

        this.mPi = false;
    }

    public CoordinateSystem(final Range xRange, final Range yRange, final Point size, final List<Integer> margin, final String xUnit, final String yUnit) {
        this(xRange, yRange, size, margin, false, xUnit, yUnit);
    }

    /**
     * Constructor for a coordinate system with metric axes. TODO replace by a
     * factory in order to avoid code duplication
     *
     * @param xRange Range
     * @param yRange Range
     * @param size Point
     * @param diagramContentMargin List(Integer)
     * @param pi boolean
     * @param xUnit String
     * @param yUnit String
     */
    public CoordinateSystem(final Range xRange, final Range yRange, final Point size, final List<Integer> diagramContentMargin, final boolean pi, final String xUnit, final String yUnit) {
        mOrigin = new Point(diagramContentMargin.get(CONSTANT), diagramContentMargin.get(0));

        this.mSize = new Point(size);
        this.mSize.setX(this.mSize.getX() - (diagramContentMargin.get(1) + diagramContentMargin.get(CONSTANT)));
        this.mSize.setY(this.mSize.getY() - (diagramContentMargin.get(0) + diagramContentMargin.get(2)));
        // this.mSize.mX = Math.min(this.mSize.mX, this.mSize.mY);
        // this.mSize.mY = this.mSize.mX;

        mXAxis = new MetricAxis(xRange, this.mSize.getX(), xRange.getName(), xUnit);
        mYAxis = new MetricAxis(yRange, this.mSize.getY(), yRange.getName(), yUnit);

        this.mPi = pi;
    }

    /**
     * Converts a point from virtual to real coordinates.
     *
     * @param x
     *            | virtual mX coordinate
     * @param y
     *            | virtual mY coordinate
     * @return real point
     */
    public Point convert(final double x, final double y) {
        double newX = mOrigin.getX()
                + (x - mXAxis.mRange.getFrom()) * mSize.getX() / (mXAxis.mRange.getTo() - mXAxis.mRange.getFrom());
        double newY = mOrigin.getY() + mSize.getY()
                - ((y - mYAxis.mRange.getFrom()) * mSize.getY() / (mYAxis.mRange.getTo() - mYAxis.mRange.getFrom()));
        return new Point(newX, newY);
    }

    /**
     * Converts a point from virtual to real coordinates using an offset from the axes.
     *
     * @param x
     *            | virtual mX coordinate
     * @param y
     *            | virtual mY coordinate
     * @return real point
     */
    public Point convertWithOffset(final double x, final double y) {
        return convert(x + mXAxis.getPointOffset(), y + mYAxis.getPointOffset());
    }

    /**
     * Converts a point from virtual to real coordinates.
     *
     * @param point
     *            | virtual coordinates
     * @return real point
     */
    public Point convert(final Point point) {
        return convert(point.getX(), point.getY());
    }

    /**
     * Converts a point from virtual to real coordinates using an offset from the axes.
     *
     * @param point
     *            | virtual coordinates
     * @return real point
     */
    public Point convertWithOffset(final Point point) {
        return convertWithOffset(point.getX(), point.getY());
    }

    /**
     * Converts a point from virtual coordinates and translates it in real
     * space.
     *
     * @param x
     *            | virtual mX coordinate
     * @param y
     *            | virtual mY coordinate
     * @param dx
     *            | real mX transformation
     * @param dy
     *            | real mY transformation
     * @return real point
     */
    public Point convert(final double x, final double y, final double dx, final double dy) {
        Point real = convert(x, y);
        real.translate(dx, dy);
        return real;
    }

    /**
     * Converts a point from virtual coordinates and translates it in real
     * space.
     *
     * @param point
     *            | virtual coordinates
     * @param dx
     *            | real mX transformation
     * @param dy
     *            | real mY transformation
     * @return real point
     */
    public Point convert(final Point point, final double dx, final double dy) {
        return convert(point.getX(), point.getY(), dx, dy);
    }

    /**
     * Converts a distance on the mX axis from virtual to real.
     *
     * @param distance
     *            | virtual distance
     * @return real distance
     */
    public double convertXDistance(final double distance) {
        return distance * mSize.getX() / (mXAxis.mRange.getTo() - mXAxis.mRange.getFrom());
    }

    /**
     * Converts a distance on the mY axis from virtual to real.
     *
     * @param distance
     *            | virtual distance
     * @return real distance
     */
    public double convertYDistance(final double distance) {
        return distance * mSize.getY() / (mYAxis.mRange.getTo() - mYAxis.mRange.getFrom());
    }

    /**
     * Converts two virtual points and calculates their real distance.
     *
     * @param point1 Point
     * @param point2 Point
     * @return real distance
     */
    public double convertDistance(final Point point1, final Point point2) {
        return convert(point1).distance(convert(point2));
    }

    /**
     * Formats the mX value of a point with respect to if Pi is set in the
     * coordinate system.
     *
     * @param x
     *            mX-value
     * @return formated string for the point
     */
    public String formatX(final double x) {
        String str = mXAxis.formatForAxisLabel(x);
        if (mPi && !"0".equals(str)) {
            str += " mPi";
        }
        return str;
    }

    /**
     * Formats the mX value of a point with respect to if Pi is set in the
     * coordinate system, for axis audio labels.
     *
     * @param x
     *            mX-value
     * @return formated string for the point
     */
    public String formatXForAxisSpeech(final double x) {
        String str = mXAxis.formatForAxisAudioLabel(x);
        if (mPi && !"0".equals(str)) {
            str += " mPi";
        }
        return str;
    }

    /**
     * Formats the mX value of a point with respect to if Pi is set in the
     * coordinate system, for mSymbol audio labels.
     *
     * @param x
     *            mX-value
     * @return formated string for the point
     */
    public String formatXForSymbolSpeech(final double x) {
        String str = mXAxis.formatForSymbolAudioLabel(x);
        if (mPi && !"0".equals(str)) {
            str += " mPi";
        }
        return str;
    }

    /**
     * Formats the mY value of a point.
     *
     * @param y
     *            mY-value
     * @return formated string for the point
     */
    public String formatY(final double y) {
        return mYAxis.formatForAxisLabel(y);
    }

    /**
     * Formats a Point that it is optimized for speech output. E.g. (mX / mY)
     *
     * @param point
     *            The point that should be transformed into a textual
     *            representation
     * @return formated string for the point with '/' as delimiter
     */
    public String formatForSpeech(final Point point) {
        if (point.getName() != null && !point.getName().isEmpty()) {
            return point.getName() + " " + formatXForSymbolSpeech(point.getX()) + " / " + formatY(point.getY());
        } else {
            return "" + formatXForSymbolSpeech(point.getX()) + " / " + formatY(point.getY());
        }

    }

    /**
     * Formats a Point that it is optimized for speech output for an axis audio label.
     *
     * @param point
     *            The point that should be transformed into a textual
     *            representation
     * @return formated string for the point with '/' as delimiter
     */
    public String formatForAxisSpeech(final Point point) {
        if (point.getName() != null && !point.getName().isEmpty()) {
            return point.getName() + " " + formatXForAxisSpeech(point.getX()) + " / " + formatY(point.getY());
        } else {
            return "" + formatXForAxisSpeech(point.getX()) + " / " + formatY(point.getY());
        }
    }
}
