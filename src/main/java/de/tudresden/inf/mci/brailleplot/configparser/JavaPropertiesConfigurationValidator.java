package de.tudresden.inf.mci.brailleplot.configparser;


import de.tudresden.inf.mci.brailleplot.rendering.language.BrailleLanguage;
import de.tudresden.inf.mci.brailleplot.util.GeneralResource;
import de.tudresden.inf.mci.brailleplot.printerbackend.PrinterCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Concrete validator for properties parsed from configuration files in Java Property File format.
 * @author Leonard Kupper, Andrey Ruzhanskiy
 * @version 2019-09-13
 */
public final class JavaPropertiesConfigurationValidator implements ConfigurationValidator {

    private final String mPrinterPrefix = "printer";
    private final String mRepresentationPrefix = "representation";
    private final String mFormatPrefix = "format";

    private final HashMap<String, Predicate<String>> mValidPrinterProperties = new HashMap<>();
    private final HashMap<String, Predicate<String>> mValidRepresentationProperties = new HashMap<>();
    private final HashMap<String, Predicate<String>> mValidFormatProperties = new HashMap<>();
    private final ArrayList<String> mRequiredPrinterProperties = new ArrayList<>();
    private final ArrayList<String> mRequiredRepresentationProperties = new ArrayList<>();
    private final ArrayList<String> mRequiredFormatProperties = new ArrayList<>();
    private String mSearchPath;

    Logger mLogger = LoggerFactory.getLogger(this.getClass());

    public JavaPropertiesConfigurationValidator() {

        // Definition of type checker predicates
        Predicate<String> requireEmpty = String::isEmpty;
        Predicate<String> requireNotEmpty = requireEmpty.negate();
        Predicate<String> requireInteger = JavaPropertiesConfigurationValidator::checkIfInteger;
        Predicate<String> requireNonZero = JavaPropertiesConfigurationValidator::checkIfNonZero;
        Predicate<String> requireDouble = JavaPropertiesConfigurationValidator::checkIfDouble;
        Predicate<String> requireBoolean = JavaPropertiesConfigurationValidator::checkIfBoolean;
        Predicate<String> requirePositive = JavaPropertiesConfigurationValidator::checkIfPositive;

        // Definition of valid printer properties
        definePrinterProperty("name", requireNotEmpty);
        definePrinterProperty("mode", requireNotEmpty);
        definePrinterProperty("brailletable", requireNotEmpty, false);  // checked in interpretation
        definePrinterProperty("semantictable", requireNotEmpty, false); // before predicate validation
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
        defineFormatProperty("page.width", requireInteger.and(requirePositive));
        defineFormatProperty("page.height", requireInteger.and(requirePositive));
        defineFormatProperty("margin.top", requireInteger.and(requirePositive));
        defineFormatProperty("margin.right", requireInteger.and(requirePositive));
        defineFormatProperty("margin.bottom", requireInteger.and(requirePositive));
        defineFormatProperty("margin.left", requireInteger.and(requirePositive));

        // Definition of valid representation properties
        defineRepresentationProperty("general.nonexistentDataText", requireNotEmpty);
        defineRepresentationProperty("general.brailleLanguage", requireNotEmpty);
        defineRepresentationProperty("general.legendKeyword", requireNotEmpty);
        defineRepresentationProperty("general.maxTitleHeight", requireInteger.and(requirePositive).and(requireNonZero));
        defineRepresentationProperty("rasterize.barChart.maxBarThickness", requireInteger.and(requirePositive));
        defineRepresentationProperty("rasterize.barChart.minBarThickness", requireInteger.and(requirePositive));
        defineRepresentationProperty("rasterize.barChart.padding.title", requireInteger);
        defineRepresentationProperty("rasterize.barChart.padding.caption", requireInteger);
        defineRepresentationProperty("rasterize.barChart.padding.groups", requireInteger);
        defineRepresentationProperty("rasterize.barChart.padding.bars", requireInteger);
        defineRepresentationProperty("floatingDot.minBarWidth", requireDouble.and(requirePositive), false);
        defineRepresentationProperty("floatingDot.maxBarWidth", requireDouble.and(requirePositive), false);
        defineRepresentationProperty("floatingDot.minBarDist", requireDouble.and(requirePositive), false);
        defineRepresentationProperty("floatingDot.secondAxis", requireBoolean, false);
        defineRepresentationProperty("floatingDot.frames", requireBoolean, false);
        defineRepresentationProperty("floatingDot.derivation", requireBoolean, false);
        defineRepresentationProperty("floatingDot.grid", requireBoolean, false);
        defineRepresentationProperty("floatingDot.dotFrame", requireBoolean, false);
        defineRepresentationProperty("floatingDot.barAccumulation", requireBoolean, false);
    }

    /**
     * This method is called before validation for every property being read. It is used to interpret special properties
     * like relative file locations, do value conversions, ...
     * @param propertyName Key of the property
     * @param value Raw value read from file
     * @return Interpreted property value
     */
    private String interpretProperty(final String propertyName, final String value) throws ConfigurationValidationException {
        try {
            switch (propertyName) {
                case "brailletable":
                case "semantictable":
                    return new GeneralResource(value, mSearchPath).getResourcePath();
                case "general.brailleLanguage":
                    BrailleLanguage.Language language = BrailleLanguage.Language.valueOf(value.toUpperCase());
                    return language.toString();
                default: return value;
            }
        } catch (Exception e) {
            throw new ConfigurationValidationException("Problem while interpreting property: " + propertyName + "=" + value, e);
        }
    }

    @Override
    public void setSearchPath(final String searchPath) {
        /*
        File path = new File(searchPath);
        if (!path.isDirectory()) {
            throw new IOException("Not a directory.");
        }
        mSearchPath = path.getAbsolutePath();
         */
        mSearchPath = searchPath;
        mLogger.info("search path for file references was updated: " + searchPath);
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
     * Use this function in the validators constructor to add a representation property definition to the internal validation table.
     * The property will be treated as 'required'.
     * @param propertyName The name of the property. (The prefix 'representation.' must be omitted.)
     * @param validation The validation predicate. {@link Predicate}&lt;{@link String}&gt;
     */
    private void defineRepresentationProperty(final String propertyName, final Predicate<String> validation) {
        defineRepresentationProperty(propertyName, validation, true);
    }
    /**
     * Use this function in the validators constructor to add a representation property definition to the internal validation table.
     * @param propertyName The name of the property. (The prefix 'representation.' must be omitted.)
     * @param validation The validation predicate. {@link Predicate}&lt;{@link String}&gt;
     * @param required Signals whether this is a required property or not.
     */
    private void defineRepresentationProperty(final String propertyName, final Predicate<String> validation, final boolean required) {
        defineProperty(mValidRepresentationProperties, propertyName, validation, required, mRequiredRepresentationProperties);
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
            String interpretedValue = interpretProperty(propertyName, value);
            validationLookup(mValidPrinterProperties, propertyName, interpretedValue);
            return new PrinterProperty(propertyName, interpretedValue);
        } else if (prefix.equals(mRepresentationPrefix)) {
            if (keyParts.length <= 1) {
                throw new ConfigurationValidationException("Invalid representation property key: " + key);
            }
            String propertyName = keyParts[1];
            if (keyParts.length > 2) {
                propertyName = propertyName + "." + keyParts[2];
            }
            String interpretedValue = interpretProperty(propertyName, value);
            validationLookup(mValidRepresentationProperties, propertyName, interpretedValue);
            return new RepresentationProperty(propertyName, interpretedValue);
        } else if (prefix.equals(mFormatPrefix)) {
            if (keyParts.length <= 2) {
                throw new ConfigurationValidationException("Invalid format property key: " + key);
            }
            String formatName = keyParts[1];
            String propertyName = keyParts[2];
            String interpretedValue = interpretProperty(propertyName, value);
            validationLookup(mValidFormatProperties, propertyName, interpretedValue);
            return new FormatProperty(formatName, propertyName, interpretedValue);
        } else {
            throw new ConfigurationValidationException("Invalid property prefix: " + prefix);
        }
    }

    @Override
    public void checkPrinterConfigComplete(final Printer printerConfig) {
        checkCompleteness(printerConfig, mRequiredPrinterProperties);
    }
    @Override
    public void checkRepresentationConfigComplete(final Representation representationConfig) {
        checkCompleteness(representationConfig, mRequiredRepresentationProperties);
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

    private static boolean checkIfNonZero(final String value) {
        try {
            return (Double.parseDouble(value) != 0);
        } catch (NumberFormatException e) {
            return false;
        }
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
        throw new RuntimeException("The given mode '"
                + name + "' is currently not supported/not found in"
                + " the system. Currently, the possible values are: " + result);
    }

    /**
     * Static method for checking if the printer, which was given, exists in the Printer System Dialog.
     * @param printerName The name of the printer to check.
     * @return True if printer exists. False if not.
     */
}
