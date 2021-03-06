package de.tudresden.inf.mci.brailleplot.layout;

import de.tudresden.inf.mci.brailleplot.configparser.Format;
import de.tudresden.inf.mci.brailleplot.configparser.Printer;
import de.tudresden.inf.mci.brailleplot.configparser.Representation;
import de.tudresden.inf.mci.brailleplot.printabledata.FloatingPointData;
import de.tudresden.inf.mci.brailleplot.printabledata.SimpleFloatingPointDataImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representation of a target onto which an image can be plotted.
 * It wraps a {@link de.tudresden.inf.mci.brailleplot.printabledata.FloatingPointData} instance and describes the raster size and its layout.
 * @author Georg Graßnick and Richard Schmidt
 * @version 2019.08.26
 */
public class PlotCanvas extends AbstractCanvas<FloatingPointData<Boolean>> {

    private final Logger mLogger = LoggerFactory.getLogger(this.getClass());

    // cell distances horizontal and vertical
    private double mCellDistHor;
    private double mCellDistVer;

    // dimensions for Braille characters (e.g. axes, title)
    private double mCellHeight;
    private double mCellWidth;
    private double mDotDistHor;
    private double mDotDistVer;

    // bar dimensions
    private double mMaxBarWidth;
    private double mMinBarDist;
    private double mMinBarWidth;

    //floating dot area resolution
    private double mResolution;

    // frames for line plot
    private boolean mFrames;

    // second y-axis
    private boolean mSecondAxis;

    // scale factors
    private int mXScaleFactor;
    private int mYScaleFactor;

    // axis derivation with letters
    private boolean mAxesDerivation;

    // if grid is desired or not
    private boolean mGrid;

    // only point frames for line plots
    private boolean mDotFrame;

    // margins
    private double mMarginLeft;
    private double mMarginTop;
    private double mMarginRight;
    private double mFloatConstraintLeft;

    // Braille language
    private String mLang;

    private String mLegendKeyWord;
    private int mMaxTitleLines;

    // true if stacked bar chart
    private boolean mBarAcc;


    public PlotCanvas(final Printer printer, final Representation representation, final Format format) throws InsufficientRenderingAreaException {
        super(printer, representation, format);
    }

    public final FloatingPointData<Boolean> getNewPage() {
        mPageContainer.add(new SimpleFloatingPointDataImpl<>(mPrinter, mFormat));
        return getCurrentPage();
    }

    @Override
    public double getFullConstraintLeft() {
        return getConstraintLeft();
    }

    @Override
    public double getFullConstraintTop() {
        return getConstraintTop();
    }

    /**
     * Reads config file to get parameters to calculate class variables.
     */
    public void readConfig() {
        mLogger.trace("Reading plot specific configuration");

        mResolution = mPrinter.getProperty("floatingDot.resolution").toDouble();
        mCellWidth = mPrinter.getProperty("raster.dotDistance.horizontal").toDouble();
        mCellHeight = 2 * mPrinter.getProperty("raster.dotDistance.vertical").toDouble();
        mCellDistHor = mPrinter.getProperty("raster.cellDistance.horizontal").toDouble();
        mCellDistVer = mPrinter.getProperty("raster.cellDistance.vertical").toDouble();
        mDotDistHor = mPrinter.getProperty("raster.dotDistance.horizontal").toDouble();
        mDotDistVer = mPrinter.getProperty("raster.dotDistance.vertical").toDouble();
        mFloatConstraintLeft = mPrinter.getProperty("constraint.left").toDouble();

        mMarginLeft = mFormat.getProperty("margin.left").toDouble();
        mMarginTop = mFormat.getProperty("margin.top").toDouble();
        mMarginRight = mFormat.getProperty("margin.right").toDouble();

        mMinBarWidth = mRepresentation.getProperty("floatingDot.minBarWidth").toDouble();
        mMaxBarWidth = mRepresentation.getProperty("floatingDot.maxBarWidth").toDouble();
        mMinBarDist = mRepresentation.getProperty("floatingDot.minBarDist").toDouble();
        mSecondAxis = mRepresentation.getProperty("floatingDot.secondAxis").toBool();
        mFrames = mRepresentation.getProperty("floatingDot.frames").toBool();
        mAxesDerivation = mRepresentation.getProperty("floatingDot.derivation").toBool();
        mGrid = mRepresentation.getProperty("floatingDot.grid").toBool();
        mDotFrame = mRepresentation.getProperty("floatingDot.dotFrame").toBool();
        mLang = mRepresentation.getProperty("general.brailleLanguage").toString();
        mLegendKeyWord = mRepresentation.getProperty("general.legendKeyword").toString();
        mMaxTitleLines = mRepresentation.getProperty("general.maxTitleHeight").toInt();
        mBarAcc = mRepresentation.getProperty("floatingDot.barAccumulation").toBool();

    }

    public final double getResolution() {
        return mResolution;
    }

    public final double getCellWidth() {
        return mCellWidth;
    }

    public final double getCellHeight() {
        return mCellHeight;
    }

    public final double getCellDistHor() {
        return mCellDistHor;
    }

    public final double getCellDistVer() {
        return mCellDistVer;
    }

    public final double getDotDistHor() {
        return mDotDistHor;
    }

    public final double getDotDistVer() {
        return mDotDistVer;
    }

    public final double getMinBarWidth() {
        return mMinBarWidth;
    }

    public final double getMaxBarWidth() {
        return mMaxBarWidth;
    }

    public final double getMinBarDist() {
        return mMinBarDist;
    }

    public final boolean getSecondAxis() {
        return mSecondAxis;
    }

    public final boolean getFrames() {
        return mFrames;
    }

    public final boolean getAxesDerivation() {
        return mAxesDerivation;
    }

    public final void setXScaleFactor(final int factor) {
        mXScaleFactor = factor;
    }

    public final int getXScaleFactor() {
        return mXScaleFactor;
    }

    public final void setYScaleFactor(final int factor) {
        mYScaleFactor = factor;
    }

    public final int getYScaleFactor() {
        return mYScaleFactor;
    }

    public final boolean getGrid() {
        return mGrid;
    }

    public final double getMarginLeft() {
        return mMarginLeft;
    }

    public final double getMarginTop() {
        return mMarginTop;
    }

    public final double getMarginRight() {
        return mMarginRight;
    }

    public final double getFloatConstraintLeft() {
        return mFloatConstraintLeft;
    }

    public final boolean getDotFrame() {
        return mDotFrame;
    }

    public final String getLanguage() {
        return mLang;
    }

    public final String getLegendKeyWord() {
        return mLegendKeyWord;
    }

    public final int getMaxTitleLines() {
        return mMaxTitleLines;
    }

    public final boolean getBarAcc() {
        return mBarAcc;
    }

}
