package scraper.HighPower.scraper;

import org.openqa.selenium.WebDriver;
import scraper.HighPower.application.ApplicationSession;
import scraper.HighPower.domain.Rental;

import java.util.List;

/**
 * The abstract Scraper class serves as a base for creating web scrapers that extract rental information
 * from various sources. It provides common configuration parameters and abstract methods to be implemented by
 * subclasses.
 * <p>
 * Subclasses of Scraper must implement the scrape method to define the specific scraping logic
 * for a given rental platform.
 * </p>
 * <p>
 * This class uses configuration settings from {@code ApplicationSession} to customize the scraping process
 * according to user preferences, including geographical coordinates, maximum distance, and search criteria.
 * </p>
 *
 * @see Rental
 */
public abstract class Scraper {
    /** The wait time for loading pages, in seconds. */
    protected static final int WAIT_TIME = ApplicationSession.getWait();

    /** The latitude coordinate for filtering rental locations. */
    protected static final double LATITUDE = ApplicationSession.getLatitude();

    /** The longitude coordinate for filtering rental locations. */
    protected static final double LONGITUDE = ApplicationSession.getLongitude();

    /** The maximum distance from the specified coordinates for rental locations. */
    protected static final double MAX_DISTANCE = ApplicationSession.getMaximumDistance();

    /** The country where the search is to be conducted. */
    protected final String country = ApplicationSession.getCountry();

    /** The maximum number of people for the rental. */
    protected final int maxPeople = ApplicationSession.getPeopleQuantity();

    /** The maximum price per night for the rental. */
    protected final int maxPrice = ApplicationSession.getMaximumPrice();

    /** The search query URL. */
    protected String searchQuery;

    /**
     * Abstract method that must be implemented by subclasses to define the scraping logic.
     * <p>
     * This method uses a WebDriver instance to perform web scraping operations and extract rental
     * information.
     * </p>
     *
     * @param driver The WebDriver instance used to perform web scraping.
     * @return A list of Rental objects containing the extracted rental information.
     */
    public abstract List<Rental> scrape(WebDriver driver);
}
