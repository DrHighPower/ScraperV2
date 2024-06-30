package scraper.HighPower.domain;

import java.util.Objects;

/**
 * The Rental class represents a rental property with details such as name, URL, distance from a point,
 * price per night, and total price.
 */
public class Rental implements Comparable<Rental> {
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
    public Rental(String name, String url, double distance, double pricePerNight, double totalPrice) {
        checkNull(name);
        checkNull(url);

        checkPositive(distance, "Distance");
        checkPositive(pricePerNight, "Price per night");
        checkPositive(totalPrice, "Total price");

        this.name = name;
        this.url = url;
        this.distance = distance;
        this.pricePerNight = pricePerNight;
        this.totalPrice = totalPrice;
    }

    /**
     * Validates that the provided object is not null.
     *
     * @param obj The object to validate.
     * @throws IllegalArgumentException If the object is null.
     */
    private void checkNull(Object obj) {
        if (obj == null) throw new IllegalArgumentException("Object cannot be null!");
    }

    /**
     * Validates that the provided double is positive.
     *
     * @param value     The double to validate.
     * @param fieldName The name of the double.
     * @throws IllegalArgumentException If the double is negative.
     */
    private void checkPositive(double value, String fieldName) {
        if (value < 0) throw new IllegalArgumentException(fieldName + " must be positive!");
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

    /**
     * Compares this Rental object with another object for equality based on their name and distance.
     *
     * @param obj The object to compare with.
     * @return True if the objects are equal (have the same name and distance), false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Rental)) return false;

        Rental that = (Rental) obj;

        return that.getName().equals(this.name) && that.getDistance() == distance;
    }

    /**
     * Returns a hash code value for the object based on its url.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, distance);
    }

    /**
     * Compares this Rental object with the specified Rental object based on
     * their total prices.
     *
     * @param rental The rental to be compared.
     * @return A negative integer, zero, or a positive integer as this rental's total price
     * is less than, equal to, or greater than the specified rental's total price.
     * @throws NullPointerException If the specified rental is null.
     */
    @Override
    public int compareTo(Rental rental) {
        return Double.compare(this.totalPrice, rental.totalPrice);
    }
}
