package de.tudresden.inf.mci.brailleplot.csvparser;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Axis for displaying nominals.
 */
public class NominalAxis extends Axis {

    static final Logger log = LoggerFactory.getLogger(NominalAxis.class);

    /**
     * The categories displayed on the Axis in order.
     */
    protected List<String> categories;

    /**
     * The nominal axis gets constructed so that each tic corresponds to one
     * category. The tics are however shown
     *
     * @param categories
     * @param size
     * @param unit
     */
    public NominalAxis(List<String> categories, double size, String unit) {
        // TODO: upon implementation of vertical nominal axes set the values
        // correctly
        super(Constants.CHAR_WIDTH, 15, -10, -Constants.CHAR_WIDTH, 0.5, null, unit);

        this.categories = categories;

        double categorySize = size / categories.size();
        int maxLabelLength = 0;
        for (String label : categories)
            if (label.length() > maxLabelLength)
                maxLabelLength = label.length();
        if (categorySize < Constants.CHAR_WIDTH * (maxLabelLength + 1)) {
            log.warn(
                    "Der Platz reicht nicht aus, um Achsenbeschriftungen darzustellen. Die längste Beschriftung hat eine Länge von "
                            + maxLabelLength + " Zeichen.");
        }

        // Subdivide the axis into sections for each category
        mTicInterval = 1;
        mGridInterval = 1;
        mLabelInterval = 1;

        mTicRange = new Range(0, categories.size());
        mRange = mTicRange;
        mLabelRange = new Range(0, categories.size() - 1);

    }

    @Override
    public String formatForAxisLabel(double value) {
        int categoryNumber = (int) Math.round(value);
        if (categories.size() <= categoryNumber || categoryNumber < 0)
            return null;
        // throw new IllegalArgumentException("The selected category does not
        // exist");
        return categories.get(categoryNumber);
    }

    @Override
    public String formatForAxisAudioLabel(double value) {
        int categoryNumber = (int) Math.round(value);

        if (categoryNumber == 0) {
            return "|" + categories.get(categoryNumber);
        } else if (categoryNumber == categories.size()) {
            return categories.get(categoryNumber - 1) + "|";
        } else if (categoryNumber > 0 && categoryNumber < categories.size()) {
            return categories.get(categoryNumber - 1) + "|" + categories.get(categoryNumber);
        } else {
            return null;
        }
    }

    @Override
    public String formatForSymbolAudioLabel(double value) {
        return formatForAxisLabel(value);
    }

}
