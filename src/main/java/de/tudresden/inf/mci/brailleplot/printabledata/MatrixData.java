package de.tudresden.inf.mci.brailleplot.printabledata;

import java.util.Iterator;

/**
 * This data is used to describe the data for the "Braille" and "Graphics" print modes.
 * The data is organized in a matrix structure, which can be queried for its values on integer x (row) and y (column) indices.
 * @param <T> The type used for representing the intensity. Could be set to {@link Boolean} for basic Braille support,
 *           but could also by set to {@link Short} if different embossing strengths are required.
 * @author Georg Graßnick
 * @version 2019.06.26
 */
public interface MatrixData<T> extends PrintableData {

    /**
     * Get the value for a specific position in the matrix.
     * Indices start at 0.
     * @param row The row index of the position.
     * @param column The column index of the position.
     * @return The value at the requested position.
     */
    T getValue(int row, int column);

    /**
     * Get an iterator which iterates all dots of the matrix.
     * Depending on the width and height parameters, the iterator will iterate the dots of Braille cells of the
     * specified size from left to right and then top to bottom.
     * Example: width = 2, height = 3; matrix size is 4x6:
     *
     * 01  02  07  08
     * 03  04  09  10
     * 05  06  11  12
     * 13  14  19  20
     * 15  16  21  22
     * 17  18  23  24
     *
     * @param width The width of a Braille cell
     * @param height The height of a Braille cell
     * @return The according iterator.
     */
    Iterator<T> getDotIterator(int width, int height);

    /**
     * Set the value at a specific position.
     * Indices start at 0.
     * @param row The row index of the position.
     * @param column The column index of the position.
     * @param value The value to set.
     */
    void setValue(int row, int column, T value);

    /**
     * Getter.
     * @return The number of rows.
     */
    int getRowCount();

    /**
     * Getter.
     * @return The number of columns.
     */
    int getColumnCount();
}
