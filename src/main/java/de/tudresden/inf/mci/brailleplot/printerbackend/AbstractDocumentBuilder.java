package de.tudresden.inf.mci.brailleplot.printerbackend;
import de.tudresden.inf.mci.brailleplot.brailleparser.AbstractBrailleTableParser;
import de.tudresden.inf.mci.brailleplot.brailleparser.JsonParser;
import de.tudresden.inf.mci.brailleplot.brailleparser.PropertiesParser;
import de.tudresden.inf.mci.brailleplot.brailleparser.XmlParser;
import de.tudresden.inf.mci.brailleplot.configparser.Printer;
import de.tudresden.inf.mci.brailleplot.printabledata.MatrixData;

/**
 * This class provides an extension point for further implementation
 * and protocol building for documents that need to be send to the printer.
 * The common Interface is the getDocument() and assemble() method.
 * Its usable for all braille printers.
 * @param <T> Type of MatrixData.
 * @author Andrey Ruzhanskiy
 * @version 28.05.2019
 */

public abstract class AbstractDocumentBuilder<T> {

    MatrixData<T> mData;


    AbstractBrailleTableParser mParser;

    /**
     * Complex method for complex construction of an document for the printer.
     * @param data Raw data to be printed without any escapes equences
     * @return Fully build document as byte[]
     */
    abstract byte[] assemble(MatrixData<T> data);

    /**
     * Method for setting the correct parser. Reads the file from the printer configuration, then checks
     * if the file extension is supported.
     * @throws NotSupportedFileExtensionException If the file extension is not supported.
     */
    void setParser() throws NotSupportedFileExtensionException {
        //read braille table path
        Printer printer = mData.getPrinterConfig();
        String brailleTablePath = printer.getProperty("brailletable").toString();

        //read which kind of parser is needed (properties, json, xml,...)
        String fileEnding = brailleTablePath.split("\\.")[1];
        switch (fileEnding) {
            case "properties": mParser = new PropertiesParser(printer.getProperty("brailletable").toString()); break;
            case "json": mParser = new JsonParser(printer.getProperty("brailletable").toString()); break;
            case "xml": mParser = new XmlParser(printer.getProperty("brailletable").toString()); break;
            default: throw new NotSupportedFileExtensionException("The Fileextension " + fileEnding + "is currently not supported.");
        }
    }
}
