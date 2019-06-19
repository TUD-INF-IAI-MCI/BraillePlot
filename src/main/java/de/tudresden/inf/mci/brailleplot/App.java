package de.tudresden.inf.mci.brailleplot;

import de.tudresden.inf.mci.brailleplot.exporter.PrintDirector;
import de.tudresden.inf.mci.brailleplot.exporter.PrinterConfiguration;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Main class.
 * Set up the application and run it.
 * @author Georg Graßnick, Andrey Ruzhanskiy
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

    private ConcurrentLinkedDeque<Runnable> mFinalizers;

    private App() {
        sInstance = this;
        mFinalizers = new ConcurrentLinkedDeque<>();
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
        getInstance().mFinalizers.add(r);
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
    public static void terminateWithException(final Exception e) {
        terminateWithException(e, "");
    }

    /**
     * Terminate the complete Application in case of an untreatable error.
     * @param e The Exception that led to the error.
     * @param message An additional message to print to stderr.
     */
    public static void terminateWithException(final Exception e, final String message) {
        if (!message.isEmpty()) {
            System.err.println(message);
        }
        e.printStackTrace();
        getInstance().runFinalizers();
        System.exit(EXIT_ERROR);
    }

    /**
     * Main loop of the application.
     * @param args Command line parameters.
     * @return 0 if Application exited successfully, 1 on error.
     */
    int run(final String[] args) {

        registerFinalizer(() -> {
            System.out.println("Application terminated.");
        });

        try {
            // Parse command line parameters


            // If requested, print help and exit


            // Parse csv data

            // ...



            // Last Step: Printing


            if (PrintDirector.printerExists("Index Everest-D V4")) {
                System.out.println("Ja");
            } else {
                System.out.println("Nein");
            }

            PrintDirector printD = new PrintDirector(PrinterConfiguration.NORMALPRINTER);
            /*
            byte[] data = lt.buildDemo(1);
            lt.printString(data);
            */

        } catch (final Exception e) {
            terminateWithException(e);
        } finally {
            runFinalizers();
        }

        return EXIT_SUCCESS;
    }


}
