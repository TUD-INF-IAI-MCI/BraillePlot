package de.tudresden.inf.mci.brailleplot.diagrams;

import de.tudresden.inf.mci.brailleplot.datacontainers.Named;
import de.tudresden.inf.mci.brailleplot.datacontainers.PointContainer;
import de.tudresden.inf.mci.brailleplot.datacontainers.PointList;
import de.tudresden.inf.mci.brailleplot.datacontainers.PointListContainer;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Representation of a bar chart with basic data functions. Implements Renderable.
 * @author Richard Schmidt, Georg Graßnick
 * @version 2019.09.02
 */
public class BarChart extends Diagram {
    private PointListContainer<PointList> mData;

    public BarChart(final PointListContainer<PointList> data) {
        Objects.requireNonNull(data);
        mData = data;
    }

    /**
     * Getter for the category names in a list.
     *
     * @return list with category names as strings
     */
    public List<String> getCategoryNames() {
        return mData.stream()
                .map(Named::getName)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Getter for the minimum y-value.
     *
     * @return double minimum y-value
     */
    public double getMinY() {
        return mData.getMinY();
    }

    /**
     * Getter for the maximum y-value.
     *
     * @return double maximum y-value
     */
    public double getMaxY() {
        return mData.getMaxY();
    }

    /**
     * Getter for a list with x-y-Pairs: x is the index (always just counts from 0 up), y is the value.
     * @return PointList with the corresponding data set
     */
    public PointContainer<PointList> getDataSet() {
        return mData;
    }
}

