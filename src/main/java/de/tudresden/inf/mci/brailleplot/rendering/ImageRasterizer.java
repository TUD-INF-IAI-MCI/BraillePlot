package de.tudresden.inf.mci.brailleplot.rendering;

import de.tudresden.inf.mci.brailleplot.layout.RasterCanvas;
import de.tudresden.inf.mci.brailleplot.layout.Rectangle;
import de.tudresden.inf.mci.brailleplot.printabledata.MatrixData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

import static java.lang.Math.min;
import static java.lang.Math.round;

/**
 * A rasterizer that is able to re-raster a raster graphics onto a canvas.
 * @author Leonard Kupper
 * @version 2019.07.22
 */
public class ImageRasterizer implements Rasterizer<Image> {

    // This Rasterizer is a basic example on how to create a rasterizer.
    // Each rasterizer is the implementation of the Rasterizer interface. To create a rasterizer, implement the
    // Rasterizer interface by overriding the rasterize method. Since Rasterizer is a functional interface,
    // it is technically also possible to implement a rasterizer as a method independent from a class, as long
    // as it takes a renderable and a canvas as parameters.

    private final Logger mLogger = LoggerFactory.getLogger(this.getClass());

    // Switches
    private boolean mPreventOverStretch;
    private boolean mPreserveAspectRatio;
    private boolean mQuantifiedPositions;

    // Output threshold: A dot will be set for gray scale values below (darker than/equal) this threshold.
    private int mLowThreshold;
    private static final int DEFAULT_THRESHOLD = 80;

    /**
     * Constructor. Creates a new {@link Rasterizer} for instances of {@link Image} with default settings.
     */
    public ImageRasterizer() {
        this(true, true, true, DEFAULT_THRESHOLD);
        mLogger.trace("ImageRasterizer has been setup with default settings");
    }

    /**
     * Constructor. Creates a new {@link Rasterizer} for instances of {@link Image}.
     * @param preventOverStretch In case that the given images resolution is smaller than the grid on at least one
     *                           dimension this flag prevents it to be 'stretched' on the output, leading to 'cuts' in
     *                           former solid lines.
     * @param preserveAspectRatio  This flag will cause the rasterizer to select the smaller of both scaling ratios for
     *                             both dimensions to keep aspect ratio the same in the output.
     * @param useQuantifiedPositions Algorithm selection flag:
     *                               If set to true, output dot positions will be quantified.
     *                               If set to false, simple linear mapping will be applied instead.
     * @param threshold Gray scale threshold which determines whether a pixel in the original image sets a dot in the output.
     */
    public ImageRasterizer(
            final boolean preventOverStretch,
            final boolean preserveAspectRatio,
            final boolean useQuantifiedPositions,
            final int threshold) {
        mPreventOverStretch = preventOverStretch;
        mPreserveAspectRatio = preserveAspectRatio;
        mQuantifiedPositions = useQuantifiedPositions;
        mLowThreshold = threshold;
        mLogger.trace("Created ImageRasterizer. Settings: preventOverStretch={}, preserveAspectRatio={}, useQuantifiedPositions={}, lowThreshold={}",
                mPreventOverStretch, mPreserveAspectRatio, mQuantifiedPositions, mLowThreshold);
    }

    /**
     * Rasterizes a {@link Image} instance onto a {@link RasterCanvas}.
     * @param imgData A instance of {@link Image} representing the renderable image.
     * @param canvas A instance of {@link RasterCanvas} representing the target for the rasterizer output.
     */
    @Override
    public void rasterize(final Image imgData, final RasterCanvas canvas) {

        // Each rasterizer essentially works by taking an instance of a Renderable (in this case Image) and then
        // creating a graphical representation of the object on the raster canvas.

        // In this example a raster image will be rasterized onto the canvas by two selectable methods:
        // - linear mapping
        // - quantified positions

        // Both methods are similar, since the given image is already represented on a raster, which means that the
        // basic question is just how to map from one raster to the other. This is were the methods differ.
        // Another question is when to set a dot, which in this example is done by a simple threshold for
        // the grey scale value of each pixel.
        // A more sophisticated implementation could utilize an edge finding algorithm.

        mLogger.info("Rasterizing Image {}", imgData);

        mLogger.trace("Retrieving buffered image");
        // First, a readable representation of the is retrieved.
        BufferedImage imgBuf = imgData.getBufferedImage();

        // Then the selected method is applied.
        // Implementing the rasterizer as class comes in handy. Subtasks can be splitted into help
        // methods and different modular rasterizers can be reused by calling them inside the rasterize method.
        if (mQuantifiedPositions) {
            quantifiedPositionMapping(imgBuf, canvas);
        } else {
            linearMapping(imgBuf, canvas);
        }
    }

    private void linearMapping(final BufferedImage imgBuf, final RasterCanvas canvas) {

        mLogger.trace("Applying linear mapping algorithm");

        // A canvas is basically a wrapper for multiple representations of printable data, each representing a page.
        // These representations can be acquired by either requesting the current page or creating a new page.
        MatrixData<Boolean> data = canvas.getNewPage();

        // The raster canvas delivers meta information about the underlying format and raster geometry.
        // A vital information are the cell- and dot-rectangle of the canvas. Every rasterizer should take care of at
        // least these rectangles which represent the portion of the raster that the rasterizer is expected to work on.
        Rectangle availableArea = canvas.getDotRectangle();
        // A rasterizer trying to set values outside of the dot rectangle will cause an exception.
        // Other information about the grid (spacing of cells and dots, ...) is available but must not always be
        // regarded, depending on the use case.

        // Calculate the ratios between original image and target raster. (resolution 'shrink' factor)
        double hRatio =  (availableArea.getWidth() - 1) / imgBuf.getWidth();
        double vRatio = (availableArea.getHeight() - 1) / imgBuf.getHeight();

        if (mPreventOverStretch) {
            // In case that the given images resolution is smaller than the grid on at least one dimension
            // this prevents it to be 'stretched' on the output, leading to 'cuts' in former solid lines.
            // The maximum ratio is 1 for the linear mapping, meaning that the pixel position would be the
            // exact dot position in the output.
            // A ratio smaller than 1 would mean that some pixels will be mapped to the same dot to fit the output,
            // but without any regard to the spacing between the single dots.
            hRatio = min(hRatio, 1);
            vRatio = min(hRatio, 1);
        }
        if (mPreserveAspectRatio) {
            // This selects the smaller of both ratios for both dimensions to keep aspect ratio the same in the output.
            hRatio = min(hRatio, vRatio);
            vRatio = min(vRatio, vRatio);
        }

        // Linear Mapping: The pixel position of the original image is linearly mapped to the dot position.
        // This can lead to distortions because the original pixel raster is equidistant, but the output raster
        // does not have to be equidistant.

        mLogger.trace("Starting scanning through image pixel values...");
        // Scan through each pixel of the original image
        for (int x = 0; x < imgBuf.getWidth(); x++) {
            // Convert from original pixel x-position to braille dot x-position.
            // Linear mapping: The conversion happens disregarding the grid spacing (dot and cell distances)
            int column = (int) round(hRatio * (x + 1));
            for (int y = 0; y < imgBuf.getHeight(); y++) {
                // Convert from original pixel y-position to braille dot x-position.
                int row = (int) round(vRatio * (y + 1));
                // Calculate gray scale value and compare against threshold.
                int value = toGrayScaleValue(imgBuf.getRGB(x, y));
                if (value <= mLowThreshold) {
                    data.setValue(row, column, true);
                }
            }
        }
        mLogger.trace("Finished scanning the image");
    }

    private void quantifiedPositionMapping(final BufferedImage imgBuf, final RasterCanvas canvas) {

        mLogger.trace("Applying quantified position algorithm");

        MatrixData<Boolean> data = canvas.getNewPage();

        // Instead of using the dot rectangle a rectangle representing the target printing space in millimeters
        // is built from the canvas information.
        Rectangle availableArea = new Rectangle(0, 0, canvas.getPrintableWidth(), canvas.getPrintableHeight());
        mLogger.trace("Determined available area (in mm): {}", availableArea);

        // Calculate the ratios between original image and target printable area. (mm / pixel)
        double hRatio =  (availableArea.getWidth() / imgBuf.getWidth());
        double vRatio = (availableArea.getHeight() / imgBuf.getHeight());

        if (mPreventOverStretch) {
            // Here, the maximum ratio is not 1 as in the linear mapping but instead equal to the regarding dot
            // distances. This is because the ratios are not measured in dots/pixel but mm/pixel.
            hRatio = min(hRatio, canvas.getHorizontalDotDistance());
            vRatio = min(vRatio, canvas.getVerticalDotDistance());
        }
        if (mPreserveAspectRatio) {
            hRatio = min(hRatio, vRatio);
            vRatio = min(hRatio, vRatio);
        }

        // Quantified Positions: The pixel position of the original image is linearly mapped to the respective
        // millimeter position on the printed area of the page. This step preserves the original distance ratios.
        // In a second step, the calculated exact position is quantified to fit a dot position on the raster.
        // Distortions can still be introduced but are minimized.

        mLogger.trace("Staring scanning through image pixel values...");
        // Scan through all pixels of the original image
        for (int x = 0; x < imgBuf.getWidth(); x++) {
            // Convert from original pixel x-position to printed dot x-position in millimeters.
            // In contrast to the linear mapping, this will try to preserve the original distance ratios.
            double columnMM = hRatio * (x + 1);
            for (int y = 0; y < imgBuf.getHeight(); y++) {
                // Convert from original pixel y-position to printed dot y-position in millimeters.
                double rowMM = vRatio * (y + 1);
                // Calculate gray scale value and compare against threshold.
                int value = toGrayScaleValue(imgBuf.getRGB(x, y));
                if (value <= mLowThreshold) {
                    // The target dot position in millimeters has to be quantified regarding the raster.
                    int row = canvas.quantifyY(rowMM);
                    int column = canvas.quantifyX(columnMM);
                    data.setValue(row, column, true);
                }
            }
        }
        mLogger.trace("Finished scanning the image");
    }


    private int toGrayScaleValue(final int rgb) {
        final int colorChannels = 3;
        final int bitsPerChannel = 8;
        final int byteMask = 0xff;
        int r = (rgb >> 2 * bitsPerChannel) & byteMask;
        int g = (rgb >> bitsPerChannel) & byteMask;
        int b = (rgb) & byteMask;
        return ((r + g + b) / colorChannels);
    }
}
