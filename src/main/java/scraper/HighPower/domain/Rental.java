package scraper.HighPower.domain;

/**
 * The Rental class represents a rental property with details such as name, URL, distance from a point,
 * price per night, and total price.
 */
public class Rental {
    private final String name;
    private final String url;
    private final double distance; // In km
    private final double pricePerNight;
    private final double totalPrice;

    /**
     * Constructs a Rental object with the specified details.
     *
     * @param name          The name of the rental property.
     * @param url           The URL of the rental property.
     * @param distance      The distance of the rental property from a specific point, in kilometers.
     * @param pricePerNight The price per night for the rental property, in euros.
     * @param totalPrice    The total price for the rental property, in euros.
     */
    public Rental(String name, String url, double distance, double pricePerNight, double totalPrice){
        this.name = name;
        this.url = url;
        this.distance = distance;
        this.pricePerNight = pricePerNight;
        this.totalPrice = totalPrice;
    }

    /**
     * Gets the name of the rental property.
     *
     * @return The name of the rental property.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the URL of the rental property.
     *
     * @return The URL of the rental property.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the distance of the rental property from a specific point.
     *
     * @return The distance in kilometers.
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Gets the price per night for the rental property.
     *
     * @return The price per night.
     */
    public double getPricePerNight() {
        return pricePerNight;
    }

    /**
     * Gets the total price for the rental property.
     *
     * @return The total price.
     */
    public double getTotalPrice() {
        return totalPrice;
    }
}
