package de.tudresden.inf.mci.brailleplot.rendering;

import de.tudresden.inf.mci.brailleplot.datacontainers.PointList;
import de.tudresden.inf.mci.brailleplot.layout.InsufficientRenderingAreaException;
import de.tudresden.inf.mci.brailleplot.layout.RasterCanvas;
import de.tudresden.inf.mci.brailleplot.layout.Rectangle;
import de.tudresden.inf.mci.brailleplot.printabledata.MatrixData;
import de.tudresden.inf.mci.brailleplot.diagrams.BarChart;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static java.lang.Math.log10;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.StrictMath.ceil;

/**
 * A rasterizer for instances of {@link BarChart} which is based on an algorithm that constructs horizontal category
 * bars that are filled with a uniform texture. The rasterizer is 'cell' based, working on 6-dot or 8-dot layouts.
 * @author Leonard Kupper, Georg Graßnick
 * @version 2019.07.29
 */
public final class UniformTextureBarChartRasterizer implements Rasterizer<BarChart> {

    BarChart mDiagram;
    RasterCanvas mCanvas;
    MatrixData<Boolean> mData;

    // TODO: move some of these into the format config.
    // algorithm specific constants
    private final double[] mUnitScalings = {0.1, 0.125, 0.25, 0.5, 1.0};
    private final int mTextureUnitSize = 2; // dots
    private final int mBarMinThickness = 5; // dots
    private final int mBarMaxThickness = 9; // dots
    private final int mBarDotPadding = 1;
    private final int mExtraBarCellPadding = 0; // cells
    private final int mBarInCellPadding = 2; // dots
    private final boolean mLeftCaption = true;
    private final int mCaptionLength = 6; // cells

    // associated rasterizers
    private LiblouisBrailleTextRasterizer mTextRasterizer;
    private LinearMappingAxisRasterizer mAxisRasterizer;
    //private Rasterizer<Legend> mLegendRasterizer;

    /**
     * Constructor. Create a new rasterizer for instances of {@link BarChart}.
     */
    public UniformTextureBarChartRasterizer() {
        mAxisRasterizer = new LinearMappingAxisRasterizer();
        //mLegendRasterizer = new LegendRasterizer();
    }

    /**
     * Rasterizes a {@link BarChart} instance onto a {@link RasterCanvas}.
     * @param diagram A instance of {@link BarChart} representing the bar chart diagram.
     * @param canvas A instance of {@link RasterCanvas} representing the target for the rasterizer output.
     * @throws InsufficientRenderingAreaException If too few space is available on the {@link RasterCanvas}
     * to display the given diagram.
     */
    @Override
    public void rasterize(final BarChart diagram, final RasterCanvas canvas)
            throws InsufficientRenderingAreaException {

        // The comments here can only give a very short overview, please see the wiki for a full explanation.
        mTextRasterizer = new LiblouisBrailleTextRasterizer(canvas.getPrinter());
        mDiagram = Objects.requireNonNull(diagram);
        mCanvas = Objects.requireNonNull(canvas);
        mData = mCanvas.getCurrentPage();

        checkValidBrailleRaster();


        // Basic chart layout happens in the following steps (1-4):

        String strDiagramTitle = "I am a bar chart"; // TODO: WHERE DO WE GET THIS INFO FROM??
        Rectangle barArea = mCanvas.getCellRectangle();
        int titleBarHeight;
        Rectangle titleArea, xAxisArea;

        try {
            // 1. Reserve space for the diagram title on the top
            titleBarHeight = mTextRasterizer.calculateRequiredHeight(strDiagramTitle,
                    barArea.intWrapper().getWidth() * mCanvas.getCellWidth(), mCanvas);
            titleArea = barArea.removeFromTop(mCanvas.getCellYFromDotY(titleBarHeight));

            // 2. Reserve space for the x-axis on the bottom
            xAxisArea = barArea.removeFromBottom(2);

            // 3. Reserve space for bar captions on the left or right side
            if (mLeftCaption) {
                barArea.removeFromLeft(mCaptionLength);
            } else {
                barArea.removeFromRight(mCaptionLength);
            }

            // 4. One extra cell is removed, because there must always be one cell of space for the y-axis.
            barArea.removeFromRight(1);
        } catch (Rectangle.OutOfSpaceException e) {
            // If the rectangle throws an OutOfSpaceException, this just means that there is no way to fit all
            // components of the chart inside the given area.
            throw new InsufficientRenderingAreaException("Not enough space to build bar chart layout.", e);
        }


        // The remaining rectangle 'barArea' does now represent the available area for the bars to be displayed.


        // Now the charts value range and categories are analyzed to figure out where the y-axis (x = 0) shall be
        // placed and how to scale the bars to still fit the available space.

        double negValueRangeSize = abs(min(diagram.getMinY(), 0));
        double posValueRangeSize = max(diagram.getMaxY(), 0);
        // The complete value range is calculated in a way that it always includes zero, even if all category values
        // are positive or negative with absolute values > 0, because the y axis will always be positioned at x = 0.
        double valueRangeSize = negValueRangeSize + posValueRangeSize;

        // Calculate the amount of distinguishable units / steps (on the x-axis) that fit into the given space.
        int availableUnits = (int) floor(barArea.getWidth() * mCanvas.getCellWidth() / mTextureUnitSize);

        // The width of a single x-axis step depends on the value range and the available units that can be displayed.
        // A little helper algorithm tries to find a good scaling such that the axis divisions correspond to the original
        // value range in a rational way. (e.g. one step on the x-axis is a half of the order of magnitude of the value range.)
        double xAxisStepWidth = findAxisScaling(valueRangeSize, availableUnits);

        // Divide the bar area into a negative and a positive fragment, depending on the value range.
        int negUnits = (int) round((negValueRangeSize / valueRangeSize) * availableUnits);
        int posUnits = availableUnits - negUnits;

        // The x coordinate of the origin is (logically) set to be exactly between negative and positive range.
        int xAxisOriginPosition = barArea.intWrapper().getX() + mCanvas.getCellXFromDotX(negUnits * mTextureUnitSize);

        // Now the available vertical space is analyzed to figure out the bar thickness.
        // Again, this is done by a help algorithm.
        int availableCells = barArea.intWrapper().getHeight();
        int barThickness = findBarThickness(availableCells);



        // Now everything is ready to be rasterized onto the canvas.

        // 1. Rasterize the diagram title
        BrailleText diagramTitle = new BrailleText(strDiagramTitle, titleArea.scaledBy(mCanvas.getCellWidth(), mCanvas.getCellHeight()));
        mTextRasterizer.rasterize(diagramTitle, mCanvas);

        // 2. Draw the individual bars for each category.
        int refCellX = xAxisOriginPosition; // align bars with y-axis.
        int refCellY = barArea.intWrapper().getBottom(); // start with the bottommost bar.
        for (PointList pl : mDiagram.getDataSet()) {
            double value = pl.iterator().next().getY();
            // calculate how to represent value with the current scaling
            int barLength = (int) round(value / xAxisStepWidth) * mTextureUnitSize;
            // draw the bar including its caption and then move the reference cell y position up
            String categoryName = "foobar"; // TODO: read real name
            refCellY = drawHorizontalBar(refCellX, refCellY, barLength, barThickness, categoryName);
        }

        // 3. Rasterize both axes
        // First calculate axis positions and bounds
        // (these are conversions from 'cell-based' rectangles into a 'dot-based' representation, because the axis
        // rasterizer uses linear mapping, needs 'dot coordinates' and does not care about cell borders)
        Rectangle yAxisBound = barArea.scaledBy(mCanvas.getCellWidth(), mCanvas.getCellHeight());
        Rectangle xAxisBound = xAxisArea.scaledBy(mCanvas.getCellWidth(), mCanvas.getCellHeight());
        int originX = (xAxisOriginPosition + 1) * mCanvas.getCellWidth() - 1; // convert cell position to dot position
        int originY = xAxisBound.intWrapper().getY();

        // y-axis: no units, no tickmarks
        Axis yAxis = new Axis(Axis.Type.Y_AXIS, originX, originY, 1, 0);
        yAxis.setBoundary(yAxisBound);
        mAxisRasterizer.rasterize(yAxis, mCanvas);

        // x-axis: tickmarks for every second (full) line of the uniform texture.
        int xAxisStepDots = mTextureUnitSize * 2;
        Axis xAxis = new Axis(Axis.Type.X_AXIS, originX, originY, xAxisStepDots, 2);
        xAxis.setBoundary(xAxisBound);
        // a bit more complicated than y-axis here: building a map for the axis labels
        Map<Integer, String> xAxisLabels = new HashMap<>();
        char labelLetter = 'a';
        for (int axisTick = (negUnits / 2) * -1; axisTick <= (posUnits / 2); axisTick++) {
            xAxisLabels.put(axisTick, Character.toString(labelLetter));
            labelLetter++;
        }
        xAxis.setLabels(xAxisLabels);
        mAxisRasterizer.rasterize(xAxis, mCanvas);

        // Finished.

    }

    // Layout help methods

    @SuppressWarnings("checkstyle:MagicNumber")
    private double findAxisScaling(final double valueRangeSize, final int availableUnits) {
        double minRangePerUnit = valueRangeSize / availableUnits; // this range must fit into one 'axis step'
        double orderOfMagnitude = pow(10, ceil(log10(minRangePerUnit)));
        double scaledRange = 0;
        for (double scaling : mUnitScalings) {
            scaledRange = (scaling * orderOfMagnitude);
            if (scaledRange >= minRangePerUnit) {
                break;
            }
        }
        return scaledRange;
    }

    private int findBarThickness(final int availableCells) throws InsufficientRenderingAreaException {

        int barThickness = mBarMaxThickness;
        if ((barThickness % 2) == 0) {
            barThickness++; // Make bar thickness an uneven number. Needed for the uniform texture.
        }

        // Probe the maximum possible bar thickness
        int requiredCells;
        while (availableCells < (requiredCells = requiredCells(barThickness))) {
            barThickness -= 2;
            if (barThickness < mBarMinThickness) {
                throw new InsufficientRenderingAreaException("Not enough space to render given amount of categories in "
                        + "bar chart. " + mDiagram.getDataSet().getSize() + " categories given. " + requiredCells
                        + " cells required but only " + availableCells + " available. "
                        + "(Minimum bar thickness is set to " + mBarMinThickness + " dots)");
            }
        }
        return barThickness;
    }

    private int requiredCells(final int barThickness) {
        int cellHeight = mCanvas.getCellHeight();
        // basic thickness of single bar
        int barSize = mBarInCellPadding + barThickness;
        // additional padding between neighboring bars
        int sizeInclusive = barSize + (mExtraBarCellPadding * cellHeight) + mBarDotPadding + 1;
        // how many cells needed? -> Can borders of neighboring cells 'share' a cell?
        int barCells = (int) ceil(barSize / (double) cellHeight); // important cast, else int division happens
        int cellsInclusive = (int) ceil(sizeInclusive / (double) cellHeight);
        // --> Linear equation
        return barCells + (cellsInclusive - 1) * (mDiagram.getDataSet().getSize() - 1);
    }


    /**
     * Draws a horizontal category bar to the canvas.
     * @param cellX The reference cell x position.
     * @param cellY The reference cell y position.
     * @param length The length of the bar in dots (can also be negative).
     * @param thickness The thickness of the bar in dots.
     * @param categoryName The caption to be displayed next to the bar.
     * @return The y position of the next reference cell for the next bar.
     * @throws InsufficientRenderingAreaException If the underlying text rasterizer throws it while rasterizing the bar caption.
     */
    private int drawHorizontalBar(final int cellX, final int cellY, final int length, final int thickness,
                                  final String categoryName) throws InsufficientRenderingAreaException {
        // the bar is drawn according to the bottom right dot of the cell as reference
        int bottomRightDotX = (cellX + 1) * mCanvas.getCellWidth() - 1;
        int bottomRightDotY = (cellY + 1) * mCanvas.getCellHeight() - 1;

        // first, a rectangle is drawn between two points: 'lower' (at y-axis) and 'upper' (at bars end)
        int lowerX = bottomRightDotX;
        int lowerY = bottomRightDotY - mBarInCellPadding; // position relative to reference point
        int upperX = lowerX + length; // add the horizontal size of the bar (representing the category value)
        int upperY = lowerY - (thickness - 1); // substract the bar thickness (up = towards smaller y)
        Rasterizer.rectangle(lowerX, lowerY, upperX, upperY, mData, true);

        // then the rectangle is filled with the uniform texture
        /*
        Texture<Boolean> uniformTexture = new Texture<>(TexturedArea.BOTTOM_T_PATTERN);
        TextureRasterizer textureRasterizer = new TextureRasterizer();
        TexturedArea barFace = new TexturedArea(
                uniformTexture,
                new Rectangle(lowerX + 1, upperY + 1, length - 2, thickness - 2)
        );
        textureRasterizer.rasterize(barFace, mCanvas);
         */

        int textureStep = Integer.signum(upperX - lowerX) * mTextureUnitSize;
        int i = 0;
        for (int dotX = lowerX; dotX != upperX; dotX += textureStep) {
            // alternate between dotted and solid line
            if ((i % 2) == 0) {
                // solid line
                Rasterizer.fill(dotX, lowerY, dotX, upperY, mData, true);
            } else {
                // dotted line
                int j = 0;
                for (int dotY = lowerY; dotY > upperY; dotY--) {
                    mData.setValue(dotY, dotX, ((j % 2) == 0));
                    j++;
                }
            }
            i++;
        }

        // finally, rasterize the bar caption text
        int captionCellX, captionCellY;
        captionCellY = mCanvas.getCellYFromDotY(upperY + (thickness / 2));
        if (mLeftCaption) {
            captionCellX = mCanvas.getCellXFromDotX(min(lowerX, upperX) - 1) - mCaptionLength;
        } else {
            captionCellX = mCanvas.getCellXFromDotX(max(lowerX, upperX) + 1);
        }
        Rectangle captionArea = new Rectangle(captionCellX, captionCellY, mCaptionLength, 1);
        // TODO Something is strange gi
        mTextRasterizer.rasterize(new BrailleText(categoryName,
                captionArea.scaledBy(mCanvas.getCellWidth(), mCanvas.getCellHeight())), mCanvas);

        return mCanvas.getCellYFromDotY(upperY - (mBarDotPadding + 1)) - mExtraBarCellPadding;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    // TODO: use BrailleCell6 and BrailleCell8 class constants
    private void checkValidBrailleRaster() throws InsufficientRenderingAreaException {
        boolean isValidBrailleRaster = ((mCanvas.getCellWidth() == 2)
                && (mCanvas.getCellHeight() >= 3) && (mCanvas.getCellHeight() <= 4));
        if (!isValidBrailleRaster) {
            // TODO: Maybe refactor to have different rendering exceptions?
            throw new InsufficientRenderingAreaException("This rasterizer can only work with a 6-dot or 8-dot "
                    + "braille raster.");
        }
    }

    /*
    private boolean isEquidistantRaster() {
        return (
                (canvas.getHorizontalCellDistance() == canvas.getVerticalCellDistance())
                && (canvas.getVerticalCellDistance() == canvas.getHorizontalDotDistance())
                && (canvas.getHorizontalDotDistance() == canvas.getVerticalDotDistance())
        );
    }
     */
}