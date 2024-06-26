package scraper.HighPower.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Properties;

/**
 * The ApplicationSession class provides methods to retrieve various configuration settings
 * from a properties file. These settings include coordinates, distance, price, quantity, dates, and amenities.
 */
public class ApplicationSession {
    private static final String CONFIGURATION_FILENAME = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "config.properties";
    private static final String COUNTRY_CONFIGURATION_FILENAME = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "country.properties";
    private static final String LATITUDE_COORDINATES = "Coordinates.Latitude";
    private static final String LONGITUDE_COORDINATES = "Coordinates.Longitude";
    private static final String MEDIA_FERIAS_COUNTRY_CODE = "Coordinates.Country.MediaFerias";
    private static final String MAXIMUM_DISTANCE = "Maximum.Distance";
    private static final String MAXIMUM_PRICE = "Maximum.Price";
    private static final String PEOPLE_QUANTITY = "Quantity.People";
    private static final String NIGHT_QUANTITY = "Quantity.Night";
    private static final String START_DATE = "Date.Start";
    private static final String END_DATE = "Date.End";
    private static final String FLEXIBLE_DATE = "Date.Flexible";
    private static final String AMENITIES_POOL = "Amenities.Pool";
    private static final String PAGE_WAIT = "Element.Wait";

    /**
     * Retrieves properties from the configuration file.
     *
     * @return The properties loaded from the configuration file.
     */
    private static Properties getProperties(String filePath) {
        Properties props = new Properties();

        // Read configured values
        try {
            InputStream in = new FileInputStream(filePath);
            props.load(in);
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return props;
    }

    /**
     * Parses a double value from a string.
     *
     * @param str       The string to parse.
     * @param fieldName The name of the field for error messages.
     * @return The parsed double value.
     * @throws IllegalArgumentException If the string cannot be parsed into a double.
     */
    private static double getDoubleFromString(String str, String fieldName) {
        double value;

        try {
            value = Double.parseDouble(str);
        } catch (NullPointerException | NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " in the configuration file must be a number!");
        }

        return value;
    }

    /**
     * Parses an integer value from a string.
     *
     * @param str       The string to parse.
     * @param fieldName The name of the field for error messages.
     * @return The parsed integer value.
     * @throws IllegalArgumentException If the string cannot be parsed into an integer.
     */
    private static int getIntegerFromString(String str, String fieldName) {
        int value;

        try {
            value = Integer.parseInt(str);
        } catch (NullPointerException | NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " in the configuration file must be an integer!");
        }

        return value;
    }

    /**
     * Parses a LocalDate value from a string.
     *
     * @param str       The string to parse.
     * @param fieldName The name of the field for error messages.
     * @return The parsed LocalDate value.
     * @throws IllegalArgumentException If the string cannot be parsed into a date.
     */
    private static LocalDate getDateFromString(String str, String fieldName) {
        LocalDate date;

        try {
            date = LocalDate.parse(str, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " in the configuration file must be in the \"DD/MM/YYYY\" format!");
        }

        return date;
    }

    /**
     * Parses a boolean value from a string.
     *
     * @param str The string to parse.
     * @return The parsed boolean value.
     */
    private static boolean getBoolFromString(String str) {
        return str.toLowerCase().matches("true");
    }

    /**
     * Gets the latitude from the configuration file.
     *
     * @return The latitude.
     */
    public static double getLatitude() {
        String strLatitude = getProperties(CONFIGURATION_FILENAME).getProperty(LATITUDE_COORDINATES);
        return getDoubleFromString(strLatitude, "The latitude");
    }

    /**
     * Gets the longitude from the configuration file.
     *
     * @return The longitude.
     */
    public static double getLongitude() {
        String strLongitude = getProperties(CONFIGURATION_FILENAME).getProperty(LONGITUDE_COORDINATES);
        return getDoubleFromString(strLongitude, "The longitude");
    }

    /**
     * Gets the Media Ferias country code from the configuration file.
     *
     * @return The country code.
     */
    public static int getMediaFeriasCountryCode() {
        String strCode = getProperties(CONFIGURATION_FILENAME).getProperty(MEDIA_FERIAS_COUNTRY_CODE);
        return getIntegerFromString(strCode, "The Media Ferias country code");
    }

    /**
     * Gets the Vrbo country code from the configuration file.
     *
     * @return The country code.
     */
    public static int getVrboCountryCode(String country) {
        String strCode = getProperties(COUNTRY_CONFIGURATION_FILENAME).getProperty(country);
        return getIntegerFromString(strCode, "The Vrbo country code");
    }

    /**
     * Gets the maximum distance from the configuration file.
     *
     * @return The maximum distance.
     */
    public static double getMaximumDistance() {
        String strDistance = getProperties(CONFIGURATION_FILENAME).getProperty(MAXIMUM_DISTANCE);
        return getDoubleFromString(strDistance, "The maximum distance");
    }

    /**
     * Gets the maximum price from the configuration file.
     *
     * @return The maximum price.
     */
    public static int getMaximumPrice() {
        String strPrice = getProperties(CONFIGURATION_FILENAME).getProperty(MAXIMUM_PRICE);
        return getIntegerFromString(strPrice, "The maximum price");
    }

    /**
     * Gets the people quantity from the configuration file.
     *
     * @return The people quantity.
     */
    public static int getPeopleQuantity() {
        String strPeople = getProperties(CONFIGURATION_FILENAME).getProperty(PEOPLE_QUANTITY);
        return getIntegerFromString(strPeople, "The people quantity");
    }

    /**
     * Gets the night quantity from the configuration file.
     *
     * @return The night quantity.
     */
    public static int getNightQuantity() {
        String strNight = getProperties(CONFIGURATION_FILENAME).getProperty(NIGHT_QUANTITY);
        return getIntegerFromString(strNight, "The night quantity");
    }

    /**
     * Gets the start date from the configuration file.
     *
     * @return The start date.
     */
    public static LocalDate getStartDate() {
        String strDate = getProperties(CONFIGURATION_FILENAME).getProperty(START_DATE);
        return getDateFromString(strDate, "The start date");
    }

    /**
     * Gets the end date from the configuration file.
     *
     * @return The end date.
     */
    public static LocalDate getEndDate() {
        String strDate = getProperties(CONFIGURATION_FILENAME).getProperty(END_DATE);
        return getDateFromString(strDate, "The end date");
    }

    /**
     * Gets the flexibility of dates from the configuration file.
     *
     * @return The date flexibility.
     */
    public static boolean getFlexibility() {
        String strFlexible = getProperties(CONFIGURATION_FILENAME).getProperty(FLEXIBLE_DATE);
        return getBoolFromString(strFlexible);
    }

    /**
     * Gets the pool availability from the configuration file.
     *
     * @return The pool availability.
     */
    public static boolean getPool() {
        String strPool = getProperties(CONFIGURATION_FILENAME).getProperty(AMENITIES_POOL);
        return getBoolFromString(strPool);
    }

    /**
     * Gets the wait time from the configuration file.
     *
     * @return The wait time.
     */
    public static int getWait() {
        String strWait = getProperties(CONFIGURATION_FILENAME).getProperty(PAGE_WAIT);
        return getIntegerFromString(strWait, "The wait time");
    }
}
