package de.tudresden.inf.mci.brailleplot.configparser;


import de.tudresden.inf.mci.brailleplot.exporter.PrinterCapability;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Concrete validator for properties parsed from configuration files in Java Property File format.
 * @author Leonard Kupper
 * @version 2019.07.18
 */
class JavaPropertiesConfigurationValidator implements ConfigurationValidator {

    private final String mPrinterPrefix = "printer";
    private final String mFormatPrefix = "format";

    private final HashMap<String, Predicate<String>> mValidPrinterProperties = new HashMap<>();
    private final HashMap<String, Predicate<String>> mValidFormatProperties = new HashMap<>();
    private final HashMap<String, ArrayList<String>> mCompletenessCheck = new HashMap<>();
    private final ArrayList<String> mRequiredPrinterProperties = new ArrayList<>();
    private final ArrayList<String> mRequiredFormatProperties = new ArrayList<>();

    JavaPropertiesConfigurationValidator() {

        // Definition of type checker predicates
        Predicate<String> requireEmpty = String::isEmpty;
        Predicate<String> requireNotEmpty = requireEmpty.negate();
        Predicate<String> requireInteger = JavaPropertiesConfigurationValidator::checkIfInteger;
        Predicate<String> requireDouble = JavaPropertiesConfigurationValidator::checkIfDouble;
        Predicate<String> requireBoolean = JavaPropertiesConfigurationValidator::checkIfBoolean;
        Predicate<String> requirePositive = JavaPropertiesConfigurationValidator::checkIfPositive;
        Predicate<String> requireFileExists = JavaPropertiesConfigurationValidator::checkIfFileExists;
        Predicate<String> requireEnum = JavaPropertiesConfigurationValidator::checkIfEnum;
        Predicate<String> requirePrinterExists = JavaPropertiesConfigurationValidator::checkIfPrinterExists;

 /*
        Map<String, Predicate<String>> p = new HashMap<>();
        p.put("name", requireNotEmpty.and(requirePrinterExists));
        p.put("min.charsPerLine", requireInteger.and(requirePositive));
        p.put("max.charsPerLine", requireInteger.and(requirePositive));
        p.put("min.linesPerPage", requireInteger.and(requirePositive));
        p.put("max.linesPerPage", requireInteger.and(requirePositive));
        p.put("equidistantSupport", requireBoolean);
        p.put("min.characterDistance", requireDouble.and(requirePositive));
        p.put("max.characterDistance", requireDouble.and(requirePositive));
        p.put("min.lineDistance", requireDouble.and(requirePositive));
        p.put("max.lineDistance", requireDouble.and(requirePositive));
        p.put("brailletable", requireFileExists);
        p.put("mode", requireNotEmpty.and(requireEnum));
  */

        // Definition of valid printer properties
        Map<String, Predicate<String>> p = new HashMap<>();
        definePrinterProperty("name", requireNotEmpty.and(requirePrinterExists));
        definePrinterProperty("mode", requireNotEmpty.and(requireEnum));
        definePrinterProperty("brailletable", requireFileExists);
        definePrinterProperty("floatingDot.support", requireBoolean);
        definePrinterProperty("floatingDot.resolution", requireDouble.and(requirePositive), false);
        definePrinterProperty("constraint.top", requireDouble.and(requirePositive));
        definePrinterProperty("constraint.left", requireDouble.and(requirePositive));
        definePrinterProperty("constraint.width", requireDouble.and(requirePositive), false);
        definePrinterProperty("constraint.height", requireDouble.and(requirePositive), false);
        definePrinterProperty("raster.constraint.top", requireInteger.and(requirePositive));
        definePrinterProperty("raster.constraint.left", requireInteger.and(requirePositive));
        definePrinterProperty("raster.constraint.width", requireInteger.and(requirePositive), false);
        definePrinterProperty("raster.constraint.height", requireInteger.and(requirePositive), false);
        definePrinterProperty("raster.type", requireNotEmpty);
        definePrinterProperty("raster.dotDistance.horizontal", requireDouble.and(requirePositive));
        definePrinterProperty("raster.dotDistance.vertical", requireDouble.and(requirePositive));
        definePrinterProperty("raster.cellDistance.horizontal", requireDouble.and(requirePositive));
        definePrinterProperty("raster.cellDistance.vertical", requireDouble.and(requirePositive));
        definePrinterProperty("raster.dotDiameter", requireDouble.and(requirePositive));

        // Definition of valid format properties
        Map<String, Predicate<String>> f = new HashMap<>();
        defineFormatProperty("page.width", requireInteger.and(requirePositive));
        defineFormatProperty("page.height", requireInteger.and(requirePositive));
        defineFormatProperty("margin.top", requireInteger.and(requirePositive));
        defineFormatProperty("margin.right", requireInteger.and(requirePositive));
        defineFormatProperty("margin.bottom", requireInteger.and(requirePositive));
        defineFormatProperty("margin.left", requireInteger.and(requirePositive));

    }

    /**
     * Use this function in the validators constructor to add a printer property definition to the internal validation table.
     * The property will be treated as 'required'.
     * @param propertyName The name of the property. (The prefix 'printer.' must be omitted.)
     * @param validation The validation predicate. {@link Predicate}&lt;{@link String}&gt;
     */
    private void definePrinterProperty(final String propertyName, final Predicate<String> validation) {
        definePrinterProperty(propertyName, validation, true);
    }
    /**
     * Use this function in the validators constructor to add a printer property definition to the internal validation table.
     * @param propertyName The name of the property. (The prefix 'printer.' must be omitted.)
     * @param validation The validation predicate. {@link Predicate}&lt;{@link String}&gt;
     * @param required Signals whether this is a required property or not.
     */
    private void definePrinterProperty(final String propertyName, final Predicate<String> validation, final boolean required) {
        defineProperty(mValidPrinterProperties, propertyName, validation, required, mRequiredPrinterProperties);
    }
    /**
     * Use this function in the validators constructor to add a format property definition to the internal validation table.
     * The property will be treated as 'required'.
     * @param propertyName The name of the property. (The prefix 'format.[name].' must be omitted.)
     * @param validation The validation predicate. {@link Predicate}&lt;{@link String}&gt;
     */
    private void defineFormatProperty(final String propertyName, final Predicate<String> validation) {
        defineFormatProperty(propertyName, validation, true);
    }
    /**
     * Use this function in the validators constructor to add a format property definition to the internal validation table.
     * @param propertyName The name of the property. (The prefix 'format.[name].' must be omitted.)
     * @param validation The validation predicate. {@link Predicate}&lt;{@link String}&gt;
     * @param required Signals whether this is a required property or not.
     */
    private void defineFormatProperty(final String propertyName, final Predicate<String> validation, final boolean required) {
        defineProperty(mValidFormatProperties, propertyName, validation, required, mRequiredFormatProperties);
    }
    private void defineProperty(final HashMap<String, Predicate<String>> defTable, final String propertyName,
                                final Predicate<String> validation, final boolean required, final ArrayList<String> checkList) {
        defTable.put(propertyName, validation);
        if (required) {
            Objects.requireNonNull(checkList).add(propertyName);
        }
    }

    /**
     * This method validates a Java Property (key, value) pair against a predefined map of properties and their
     * regarding type restriction predicates. Invalid pairs will cause a ConfigurationValidationException.
     * @param key The Java Property key
     * @param value The Java Property value
     * @return A {@link ValidProperty} object representing the validated property.
     * @throws ConfigurationValidationException On any error while checking the parsed properties validity.
     */
    @Override
    public ValidProperty validate(final String key, final String value) throws ConfigurationValidationException {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        final int maxParts = 3;
        String[] keyParts = key.split("\\.", maxParts);
        String prefix = keyParts[0];

        // Decide whether printer or format property and do lookup in respective validation table.
        if (prefix.equals(mPrinterPrefix)) {
            if (keyParts.length <= 1) {
                throw new ConfigurationValidationException("Invalid printer property key: " + key);
            }
            String propertyName = keyParts[1];
            if (keyParts.length > 2) {
                propertyName = propertyName + "." + keyParts[2];
            }
            validationLookup(mValidPrinterProperties, propertyName, value);
            return new PrinterProperty(propertyName, value);
        } else if (prefix.equals(mFormatPrefix)) {
            if (keyParts.length <= 2) {
                throw new ConfigurationValidationException("Invalid format property key: " + key);
            }
            String formatName = keyParts[1];
            String namespace = mFormatPrefix + "." + formatName;
            String propertyName = keyParts[2];
            validationLookup(mValidFormatProperties, propertyName, value);
            return new FormatProperty(formatName, propertyName, value);
        } else {
            throw new ConfigurationValidationException("Invalid property prefix: " + prefix);
        }
    }

    @Override
    public void checkPrinterConfigComplete(final Printer printerConfig) {
        checkCompleteness(printerConfig, mRequiredPrinterProperties);
    }
    @Override
    public void checkFormatConfigComplete(final Format formatConfig) {
        checkCompleteness(formatConfig, mRequiredFormatProperties);
    }
    @SuppressWarnings("unchecked")
    // checklist is always of type ArrayList<String>
    public void checkCompleteness(final Configurable config, final ArrayList<String> checklist) {
        ArrayList<String> missingProperties = (ArrayList<String>) checklist.clone();
        for (String propertyName : config.getPropertyNames()) {
            // 'tick off' the existing properties.
            missingProperties.remove(propertyName);
        }
        if (!missingProperties.isEmpty()) {
            throw new IllegalStateException("Incomplete validation. Missing required properties: '" + missingProperties
                    + "' in '" + config + "'");
        }
    }

    private void validationLookup(
            final HashMap<String, Predicate<String>> validation,
            final String propertyName,
            final String value
    ) throws ConfigurationValidationException {
        // Is the property valid?
        if (!validation.containsKey(propertyName)) {
            throw new ConfigurationValidationException("Invalid property name: " + propertyName);
        }
        // Check against its type requirement predicate
        if (!validation.get(propertyName).test(value)) {
            throw new ConfigurationValidationException(
                    "Invalid value '" + value + "' for property '" + propertyName + "'"
            );
        }
    }

    // Validation Predicates

    private static boolean checkIfInteger(final String value) {
        if (!Pattern.matches("-?[0-9]+", value)) {
            return false;
        }
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private static boolean checkIfDouble(final String value) {
        //if (!Pattern.matches("-?[0-9]+", value)) {
        //    return false;
        //}
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private static boolean checkIfBoolean(final String value) {
        return Pattern.matches("(?i)^true$|^false$", value);
    }

    private static boolean checkIfPositive(final String value) {
        try {
            return (Double.parseDouble(value) >= 0);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean checkIfFileExists(final String filePath) {
        try {
            FileInputStream stream = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return true;
    }



    private static boolean checkIfEnum(final String name) {
        for (PrinterCapability p : PrinterCapability.values()) {
            if (p.toString().toLowerCase().equals(name)) {
                return true;
            }
        }
        PrinterCapability[] possibleValues = PrinterCapability.values();
        StringBuilder sBuilder = new StringBuilder();
        for (int i = 0; i < possibleValues.length; i++) {
            sBuilder.append(possibleValues[i]).append(",");
        }
        String result =  sBuilder.deleteCharAt(sBuilder.length() - 1).toString();
        throw new RuntimeException("The given mode for the printer "
                + name + "is currently not supported/not found in"
                + "the system. Currently, the possible values are: " + result);
    }

    /**
     * Static method for checking if the printer, which was given, exists in the Printer System Dialog.
     * @param printerName The name of the printer to check.
     * @return True if printer exists. False if not.
     */

    private static boolean checkIfPrinterExists(final String printerName) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service: services) {
            if (service.getName().equals(printerName)) {
                return true;
            }
        }
        return false;
    }
}
