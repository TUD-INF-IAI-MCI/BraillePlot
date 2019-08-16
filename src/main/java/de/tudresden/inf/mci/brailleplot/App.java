package de.tudresden.inf.mci.brailleplot;

import de.tudresden.inf.mci.brailleplot.configparser.Format;
import de.tudresden.inf.mci.brailleplot.configparser.JavaPropertiesConfigurationParser;
import de.tudresden.inf.mci.brailleplot.configparser.Printer;

import de.tudresden.inf.mci.brailleplot.csvparser.CsvOrientation;
import de.tudresden.inf.mci.brailleplot.csvparser.CsvParser;
import de.tudresden.inf.mci.brailleplot.csvparser.CsvType;
import de.tudresden.inf.mci.brailleplot.datacontainers.CategoricalPointListContainer;
import de.tudresden.inf.mci.brailleplot.datacontainers.PointList;
import de.tudresden.inf.mci.brailleplot.datacontainers.PointListContainer;
import de.tudresden.inf.mci.brailleplot.diagrams.LineChart;
import de.tudresden.inf.mci.brailleplot.printerbackend.PrintDirector;
import de.tudresden.inf.mci.brailleplot.printerbackend.PrinterCapability;

import de.tudresden.inf.mci.brailleplot.printabledata.MatrixData;
import de.tudresden.inf.mci.brailleplot.printabledata.SimpleMatrixDataImpl;

import de.tudresden.inf.mci.brailleplot.commandline.CommandLineParser;
import de.tudresden.inf.mci.brailleplot.commandline.SettingType;
import de.tudresden.inf.mci.brailleplot.commandline.SettingsReader;
import de.tudresden.inf.mci.brailleplot.commandline.SettingsWriter;

import de.tudresden.inf.mci.brailleplot.rendering.FunctionalRasterizer;
import de.tudresden.inf.mci.brailleplot.rendering.FunctionalRenderingBase;
import de.tudresden.inf.mci.brailleplot.rendering.LineChartRasterizer;
import de.tudresden.inf.mci.brailleplot.rendering.MasterRenderer;
import de.tudresden.inf.mci.brailleplot.rendering.RasterCanvas;
import de.tudresden.inf.mci.brailleplot.rendering.Rasterizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Main class.
 * Set up the application and run it.
 * @author Georg Graßnick, Andrey Ruzhanskiy
 * @version 06.06.19
 */

public final class App {

    /**
     * Main method.
     * Instantiate application and execute it.
     * @param args Command line parameters.
     */
    public static void main(final String[] args) {
        App app = App.getInstance();
        System.exit(app.run(args));
    }

    private static App sInstance;
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_ERROR = 1;

    private final Logger mLogger;

    private ConcurrentLinkedDeque<Runnable> mFinalizers;

    private App() {
        sInstance = this;
        mFinalizers = new ConcurrentLinkedDeque<>();
        mLogger = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Returns the instance of the singleton class.
     * @return The only class instance.
     */
    public static App getInstance() {
        if (sInstance == null) {
            return new App();
        } else {
            return sInstance;
        }
    }

    /**
     * Registers a finalizer.
     * Currently, this is an experimental feature.
     * Finalizers are run before program termination, even after exceptions.
     * Finalizers are run in reverse order of their insertion.
     * A possible use would be waiting for a logger to finish flushing the logs to disk.
     * @param r The task to perform.
     */
    public static void registerFinalizer(final Runnable r) {
        getInstance().mFinalizers.addFirst(r);
    }

    private void runFinalizers() {
        for (Runnable r : mFinalizers) {
            r.run();
        }
    }

    /**
     * Terminate the complete Application in case of an untreatable error.
     * @param e The Exception that led to the error.
     */
    private void terminateWithException(final Exception e) {
        terminateWithException(e, "");
    }

    /**
     * Terminate the complete Application in case of an untreatable error.
     * @param e The Exception that led to the error.
     * @param message An additional message to print to stderr.
     */
    private void terminateWithException(final Exception e, final String message) {
        if (!message.isEmpty()) {
            mLogger.error(message);
        }
        mLogger.error("Application will now shut down due to an unrecoverable error", e);
        runFinalizers();
        System.exit(EXIT_ERROR);
    }

    /**
     * Main loop of the application.
     * @param args Command line parameters.
     * @return 0 if Application exited successfully, 1 on error.
     */
    int run(final String[] args) {

        // Has to be the first finalizer to be added, so that it is run last
        registerFinalizer(() -> {
            mLogger.info("Application terminated");
        });

        try {
            // Logging example
            mLogger.info("Application started");

            // Parse command line parameters
            CommandLineParser cliParser = new CommandLineParser();
            SettingsWriter settings = cliParser.parse(args);
            SettingsReader settingsReader = settings;


            // If requested, print help and exit
            Optional<Boolean> printHelp = settingsReader.isTrue(SettingType.DISPLAY_HELP);
            if (printHelp.isPresent() && printHelp.get()) {
                cliParser.printHelp();
                return EXIT_SUCCESS;
            }

            // Parse csv data
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream csvStream = classloader.getResourceAsStream("examples/csv/2_line_plot.csv");
            Reader csvReader = new BufferedReader(new InputStreamReader(csvStream));

            CsvParser csvParser = new CsvParser(csvReader, ',', '\"');
            PointListContainer<PointList> container = csvParser.parse(CsvType.DOTS, CsvOrientation.HORIZONTAL);
            mLogger.debug("Internal data representation:\n {}", container.toString());
            LineChart lineChart = new LineChart(container);

            // Config Parsing

            // Check if some SpoolerService/Printservice exists
            if (!PrintDirector.isPrintServiceOn()) {
                throw new Exception("Can't find any Printservices on this System.");
            }

            // Parse properties File
            Optional<String> configPath = settingsReader.getSetting(SettingType.PRINTER_CONFIG_PATH);
            JavaPropertiesConfigurationParser configParser = new JavaPropertiesConfigurationParser(configPath.get(), "src/main/resources/config/default.properties");
            Printer printer = configParser.getPrinter();
            Format formatA4 = configParser.getFormat("A4");
            // Rasterize
            FunctionalRasterizer funcRast = new FunctionalRasterizer(LineChart.class, new LineChartRasterizer());
            FunctionalRenderingBase funcBase = new FunctionalRenderingBase();
            MasterRenderer renderer = new MasterRenderer(printer, formatA4, funcBase);
            renderer.getRenderingBase().registerRasterizer(funcRast);
            RasterCanvas canvas = renderer.rasterize(lineChart);
            // Last Step: Printing
            @SuppressWarnings("checkstyle:MagicNumber")
            MatrixData<Boolean> data = new SimpleMatrixDataImpl<>(printer, formatA4, 18, 20, true);
            String printerConfigUpperCase = printer.getProperty("mode").toString().toUpperCase();
            PrintDirector printD = new PrintDirector(PrinterCapability.valueOf(printerConfigUpperCase), printer);
            printD.print(data);

        } catch (final Exception e) {
            terminateWithException(e);
        }

        runFinalizers();

        return EXIT_SUCCESS;
    }
}
