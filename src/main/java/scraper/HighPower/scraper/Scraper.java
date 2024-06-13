package scraper.HighPower.scraper;

import scraper.HighPower.domain.Rental;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Properties;

/**
 * Abstract class representing a generic scraper for rental properties.
 * This class provides methods to retrieve configuration properties
 * such as coordinates, price, dates, and other search parameters from a properties file.
 */
abstract public class Scraper {
    private static final String CONFIGURATION_FILENAME = "src/main/resources/config.properties";
    private static final String LATITUDE_COORDINATES = "Coordinates.Latitude";
    private static final String LONGITUDE_COORDINATES = "Coordinates.Longitude";
    private static final String MAXIMUM_DISTANCE = "Maximum.Distance";
    private static final String MAXIMUM_PRICE = "Maximum.Price";
    private static final String NIGHT_QUANTITY  = "Quantity.Night";
    private static final String PEOPLE_QUANTITY = "Quantity.People";
    private static final String START_DATE = "Date.Start";
    private static final String END_DATE = "Date.End";
    private static final String AMENITIES_POOL = "Amenities.Pool";

    /**
     * Retrieves properties from the configuration file.
     *
     * @return The properties loaded from the configuration file.
     */
    private Properties getProperties() {
        Properties props = new Properties();

        // Read configured values
        try {
            InputStream in = new FileInputStream(CONFIGURATION_FILENAME);
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
     * @param str The string to parse.
     * @param fieldName The name of the field for error messages.
     * @return The parsed double value.
     * @throws IllegalArgumentException If the string cannot be parsed into a double.
     */
    private double getDoubleFromString(String str, String fieldName){
        double value;

        try {
            value = Double.parseDouble(str);
        }catch (NullPointerException | NumberFormatException e){
            throw new IllegalArgumentException(fieldName + " in the configuration file must be a number!");
        }

        return value;
    }

    /**
     * Parses an integer value from a string.
     *
     * @param str The string to parse.
     * @param fieldName The name of the field for error messages.
     * @return The parsed integer value.
     * @throws IllegalArgumentException If the string cannot be parsed into an integer.
     */
    private int getIntegerFromString(String str, String fieldName){
        int value;

        try {
            value = Integer.parseInt(str);
        }catch (NullPointerException | NumberFormatException e){
            throw new IllegalArgumentException(fieldName + " in the configuration file must be an integer!");
        }

        return value;
    }

    /**
     * Parses a LocalDate value from a string.
     *
     * @param str The string to parse.
     * @param fieldName The name of the field for error messages.
     * @return The parsed LocalDate value.
     * @throws IllegalArgumentException If the string cannot be parsed into a date.
     */
    private LocalDate getDateFromString(String str, String fieldName){
        LocalDate date;

        try {
            date = LocalDate.parse(str, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }catch (DateTimeParseException e){
            throw new IllegalArgumentException(fieldName + " in the configuration file must be in the \"DD/MM/YYYY\" format!");
        }

        return date;
    }

    /**
     * Gets the latitude from the configuration file.
     *
     * @return The latitude.
     */
    protected double getLatitude() {
        String strLatitude = getProperties().getProperty(LATITUDE_COORDINATES);
        return getDoubleFromString(strLatitude, "The latitude");
    }

    /**
     * Gets the longitude from the configuration file.
     *
     * @return The longitude.
     */
    protected double getLongitude() {
        String strLongitude = getProperties().getProperty(LONGITUDE_COORDINATES);
        return getDoubleFromString(strLongitude, "The longitude");
    }

    /**
     * Gets the maximum distance from the configuration file.
     *
     * @return The maximum distance.
     */
    protected double getMaximumDistance() {
        String strDistance = getProperties().getProperty(MAXIMUM_DISTANCE);
        return getDoubleFromString(strDistance, "The maximum distance");
    }

    /**
     * Gets the maximum price from the configuration file.
     *
     * @return The maximum price.
     */
    protected double getMaximumPrice() {
        String strPrice = getProperties().getProperty(MAXIMUM_PRICE);
        return getDoubleFromString(strPrice, "The maximum price");
    }

    /**
     * Gets the night quantity from the configuration file.
     *
     * @return The night quantity.
     */
    protected int getNightQuantity() {
        String strNight = getProperties().getProperty(NIGHT_QUANTITY);
        return getIntegerFromString(strNight, "The night quantity");
    }

    /**
     * Gets the people quantity from the configuration file.
     *
     * @return The people quantity.
     */
    protected int getPeopleQuantity() {
        String strPeople = getProperties().getProperty(PEOPLE_QUANTITY);
        return getIntegerFromString(strPeople, "The people quantity");
    }

    /**
     * Gets the start date from the configuration file.
     *
     * @return The start date.
     */
    protected LocalDate getStartDate() {
        String strDate = getProperties().getProperty(START_DATE);
        return getDateFromString(strDate, "The start date");
    }

    /**
     * Gets the end date from the configuration file.
     *
     * @return The end date.
     */
    protected LocalDate getEndDate() {
        String strDate = getProperties().getProperty(END_DATE);
        return getDateFromString(strDate, "The end date");
    }

    /**
     * Calculates the Haversine distance between two points on the Earth specified by latitude and longitude.
     *
     * @param lat1 Latitude of the first point.
     * @param lon1 Longitude of the first point.
     * @param lat2 Latitude of the second point.
     * @param lon2 Longitude of the second point.
     * @return The Haversine distance between the two points in kilometers.
     */
    private static double haversine(double lat1, double lon1,
                                    double lat2, double lon2){
        // Distance between latitudes and longitudes
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // Convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Apply formulae
        double a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.pow(Math.sin(dLon / 2), 2) *
                        Math.cos(lat1) *
                        Math.cos(lat2);
        double rad = 6371;
        double c = 2 * Math.asin(Math.sqrt(a));
        return rad * c;
    }

    /**
     * Abstract method to be implemented by subclasses for scraping rental properties.
     *
     * @return A list of rental properties.
     */
    public abstract List<Rental> scrape();
}
