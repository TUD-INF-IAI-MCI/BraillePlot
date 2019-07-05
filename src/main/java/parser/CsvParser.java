package parser;

import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;

public class CsvParser {

	static final Logger log = LoggerFactory.getLogger(CsvParser.class);
	
	private ArrayList<ArrayList<String>> csvData;

	/**
	 * Initiates the parser. The parser reads from the specified {@code reader}
	 * and populates {@link #csvData}.
	 * 
	 * @param reader
	 *            a reader, like {@link FileReader}
	 * @param separator
	 * @param quoteChar
	 * @throws IOException
	 *             if the {@link CSVReader} has problems parsing
	 */
	public CsvParser(Reader reader, char separator, char quoteChar) throws IOException {
		CSVReader csvReader = new CSVReader(reader, separator, quoteChar);

		csvData = new ArrayList<>();

		String[] nextLine;
		while ((nextLine = csvReader.readNext()) != null) {
			csvData.add(new ArrayList<String>(Arrays.asList(nextLine)));
		}

		csvReader.close();
	}
	
	public PointListList parse(CsvType csvType, CsvOrientation csvOrientation) {
		CsvParseAlgorithm csvParseAlgorithm;
		
		log.info("Parse die Daten als \"{}\", Orientierung \"{}\"", csvType, csvOrientation);
		
		switch (csvType) {
		case DOTS:
			csvParseAlgorithm = new CsvDotParser();
			break;
		case X_ALIGNED:
			csvParseAlgorithm = new CsvXAlignedParser();
			break;
		case X_ALIGNED_CATEGORIES:
			csvParseAlgorithm = new CsvXAlignedCategoriesParser();
			break;
		default:
			return null;
		}
		
		switch (csvOrientation) {
		case HORIZONTAL:
			return csvParseAlgorithm.parseAsHorizontalDataSets(csvData);
		case VERTICAL:
			return csvParseAlgorithm.parseAsVerticalDataSets(csvData);
		default:
			return null;
		}
	}
}
