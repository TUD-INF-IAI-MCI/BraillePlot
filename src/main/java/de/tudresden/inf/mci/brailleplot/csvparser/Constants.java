package de.tudresden.inf.mci.brailleplot.csvparser;

import de.tudresden.inf.mci.brailleplot.datacontainers.Point;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Arrays;

/**
 * Class for constants representation.
 */
public final class Constants {
    public static final String PROPERTIES_FILENAME = "svgplot.properties";
    public static final Locale LOCALE = new Locale("de");
    public static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(LOCALE);
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("Bundle");
    public static final double STROKE_WIDTH = 0.5;
    public static final List<Integer> MARGIN = Collections.unmodifiableList(Arrays.asList(15, 10, 15, 10));
    /** List of letters for function naming. */
    public static final List<String> FN_LIST = Collections
            .unmodifiableList(Arrays.asList("f", "g", "h", "i", "j", "k", "l", "m", "o", "mP", "q", "r"));
    /** List of letters for point naming. */
    public static final List<String> PN_LIST = Collections.unmodifiableList(
            Arrays.asList("A", "B", "C", "D", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "T"));
    public static final String SPACER_CSS_CLASS = "poi_symbol_bg";
    public static final DecimalFormat DECIMAL_FORMAT = getSvgDecimalFormat();
    /** The minimal distance of grid lines in mm. */
    public static final int MIN_GRID_DISTANCE = 10;
    public static final int MIN_LINE_LENGTH = 30;
    public static final Point TITLE_POSITION = new Point(Constants.MARGIN.get(3), Constants.MARGIN.get(0) + 10);
    public static final double CHAR_WIDTH = 6.5;
    public static final double TEXTURE_BORDER_DISTANCE = 2;
    public static final double TEXTURE_MIN_HEIGHT = 7;
    public static final double TEXTURE_MIN_WIDTH = 20;
    public static final double HALF_BAR_DISTANCE = 3;

    // Used for double comparisons
    public static final double EPSILON = 1E-10;

    private Constants() {
    }

    /**
     * Getter for SVG decimal format.
     * @return DecimalFormat
     */
    private static DecimalFormat getSvgDecimalFormat() {
        DecimalFormat decimalFormat = new DecimalFormat("0.###");
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        decimalFormat.setDecimalFormatSymbols(dfs);
        return decimalFormat;
    }
}
