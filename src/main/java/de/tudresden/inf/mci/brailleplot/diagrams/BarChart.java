package de.tudresden.inf.mci.brailleplot.diagrams;

import de.tudresden.inf.mci.brailleplot.datacontainers.Named;
import de.tudresden.inf.mci.brailleplot.datacontainers.PointList;
import de.tudresden.inf.mci.brailleplot.datacontainers.PointListContainer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Representation of a bar chart with basic data functions. Implements Renderable.
 * @author Richard Schmidt, Georg Graßnick
 * @version 2019.09.02
 */
public class BarChart extends Diagram {

    public BarChart(final PointListContainer<PointList> data) {
        super(data);
    }

    /**
     * Getter for the category names in a list.
     *
     * @return list with category names as strings
     */
    public List<String> getCategoryNames() {
        return getDataSet().stream()
                .map(Named::getName)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Getter for the minimum y-value.
     *
     * @return double minimum y-value
     */
    public double getMinY() {
        return getDataSet().getMinY();
    }

    /**
     * Getter for the maximum y-value.
     *
     * @return double maximum y-value
     */
    public double getMaxY() {
        return getDataSet().getMaxY();
    }

}

