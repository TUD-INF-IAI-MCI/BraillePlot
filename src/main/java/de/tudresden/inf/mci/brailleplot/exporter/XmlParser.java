package de.tudresden.inf.mci.brailleplot.exporter;


/**
 * Class representing a Xmp Parser entity.
 * @author Andrey Ruzhanskiy
 * @version 12.07.2019
 */
public class XmlParser implements AbstractBrailleTableParser {


    /**
     * Constructor for a Xmp Parser entity.
     * @param filePath
     */
    XmlParser(final String filePath) {

    }


    /**
     * Method for querying the BrailleTable.
     * @param key Braillecell, represented as String ("111000).
     * @return
     */
    @Override
    public int getValue(final String key) {
        return 0;
    }
}