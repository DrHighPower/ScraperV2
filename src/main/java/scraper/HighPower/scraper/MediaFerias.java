package scraper.HighPower.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import scraper.HighPower.application.ApplicationSession;
import scraper.HighPower.application.CalculationUtils;
import scraper.HighPower.domain.Rental;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * The MediaFerias class is responsible for scraping rental property information
 * from the MediaFerias website. It constructs search URLs based on user preferences
 * and parses the HTML content to extract rental details such as name, URL, distance,
 * price per night, and total price.
 * <p>
 * The class uses Selenium WebDriver to navigate and interact with MediaFerias' search pages
 * and JSoup to parse the HTML content.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * MediaFerias mediaFerias = new MediaFerias();
 * WebDriver driver = new ChromeDriver();
 * List<Rental> rentals = mediaFerias.scrape(driver);
 * driver.quit();
 * }
 * </pre>
 * </p>
 *
 * @see Scrapable
 * @see ApplicationSession
 * @see CalculationUtils
 */
public class MediaFerias implements Scrapable {
    private static final String URL = "https://www.mediaferias.com";
    private static final int WAIT_TIME = ApplicationSession.getWait();

    private static final double LATITUDE = ApplicationSession.getLatitude();
    private static final double LONGITUDE = ApplicationSession.getLongitude();
    private static final double MAX_DISTANCE = ApplicationSession.getMaximumDistance();
    private final String country = ApplicationSession.getCountry();
    private final int countryCode = ApplicationSession.getCountryCode();
    private final int maxPeople = ApplicationSession.getPeopleQuantity();
    private final int maxPrice = ApplicationSession.getMaximumPrice();
    private final int nightQuantity;
    private final int pool;
    private String searchQuery;
    private LocalDate[] tripDates;

    /**
     * Constructs a MediaFerias object, initializing the search parameters from the ApplicationSession.
     */
    public MediaFerias() {

        // Get the dates for the search
        LocalDate startDate = ApplicationSession.getStartDate();
        LocalDate endDate = ApplicationSession.getEndDate();

        // Get the dates
        if (ApplicationSession.getFlexibility()) {
            this.nightQuantity = ApplicationSession.getNightQuantity();

            // Because MediaFerias has no flexible dates, we need to add a new end for the search
            LocalDate newEnd = startDate.plusDays(nightQuantity + 1);

            tripDates = new LocalDate[]{startDate, newEnd};
        } else {
            this.nightQuantity = (int) DAYS.between(startDate, endDate) - 1;
            tripDates = new LocalDate[]{startDate, endDate};
        }

        // Checks if the search has pool
        if (ApplicationSession.getPool()) this.pool = 8;
        else this.pool = 0;
    }

    /**
     * Converts the number of people to a corresponding letter for the search query.
     *
     * @param peopleQuantity The number of people.
     * @return The corresponding letter for the number of people.
     */
    private String peopleQuantityToLetter(int peopleQuantity) {
        if (peopleQuantity >= 0 && peopleQuantity <= 9) {
            return String.valueOf(peopleQuantity);
        } else if (peopleQuantity >= 10 && peopleQuantity <= 35) {
            // Map numbers 10 to 35 to letters 'a' to 'z'
            return String.valueOf((char) ('a' + peopleQuantity - 10));
        }

        return "0";
    }

    /**
     * Converts the location code to a string for the search query.
     *
     * @param locationCode The location code.
     * @return The string representation of the location code.
     */
    private String locationCodeToString(int locationCode) {
        if (locationCode >= 0 && locationCode <= 9) {
            return "0" + locationCode;
        } else if (locationCode >= 10) {
            return String.valueOf(locationCode);
        }

        return "00";
    }

    /**
     * Creates the URL for the search query based on the specified parameters.
     */
    private void createURL() {
        // Creates the URL for the search
        String builder = URL + "/aluguer-ferias-" + country.toLowerCase() +
                "/" + peopleQuantityToLetter(maxPeople) +
                "00" + locationCodeToString(countryCode) +
                pool + "00/" +
                "?date1=" + tripDates[0] +
                "&date2=" + tripDates[1];

        searchQuery = builder;
    }

    /**
     * Gets the URL for a specific page of the search results.
     *
     * @param page The page number to get.
     * @return The URL for the specified page.
     */
    private String getPage(int page) {
        if (searchQuery == null) createURL();

        return searchQuery + "&cur_page=" + page;
    }

    /**
     * Extracts the distance from the rental page.
     *
     * @param url    The URL of the rental page.
     * @param driver The WebDriver instance to use.
     * @return The distance from the specified location.
     */
    private double getDistance(String url, WebDriver driver) {

        // Get the web page
        driver.get(url);

        // Load the page
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div[id='googlemap'] > script")));
        } catch (TimeoutException e) {
            System.out.println("Map not found - " + url);
            return 0;
        }

        // Get the element with the coordinates
        Document rentalDoc = Jsoup.parse(driver.getPageSource());
        Elements jsScript = rentalDoc.select("div[id='googlemap'] > script");

        // Stores the latitude and longitude of the rental
        double latitude = 0;
        double longitude = 0;

        for (Element script : jsScript) {

            // Regex matchers to get the coordinates
            Matcher latMatch = Pattern.compile("annonce_lat = +[-]?[\\d]+[.][\\d]*").matcher(script.data());
            Matcher lngMatch = Pattern.compile("annonce_lng = +[-]?[\\d]+[.][\\d]*").matcher(script.data());

            if (latMatch.find() && lngMatch.find()) {
                String latStr = latMatch.group(0).replaceAll("[^-\\d.]", "");
                String lngStr = lngMatch.group(0).replaceAll("[^-\\d.]", "");

                // Get the latitude and longitude
                latitude = Double.parseDouble(latStr);
                longitude = Double.parseDouble(lngStr);

                break; // End loop
            }
        }

        // Calculate the distance
        if (latitude == 0 && longitude == 0) return 0;
        else return CalculationUtils.haversine(LATITUDE, LONGITUDE, latitude, longitude);
    }

    /**
     * Extracts the rentals from the document.
     *
     * @param searchDoc The document to search within.
     * @param driver    The WebDriver instance to use.
     * @return A set of Rental objects extracted from the document.
     */
    private HashSet<Rental> getRentals(Document searchDoc, WebDriver driver) {

        // Gets all the rental cards
        Elements searchedRentals = searchDoc.select("div.property-bloc-autour");

        // Stores the valid rentals
        HashSet<Rental> rentals = new HashSet<>();

        // Go through all the cards containing the rentals
        for (Element rental : searchedRentals) {

            // The selectors to get the max price
            String[] selectors = {
                    "span.bloc__ribbon--info--darken > span ~ span:not(.text--sm)",
                    "span.bloc__ribbon--info--darken",
            };

            // Get the max price
            String value = "";
            for (String selector : selectors) {
                value = rental.select(selector).text();

                // Go to the next iteration if nothing was found
                if (value.isEmpty()) continue;

                // Get only the numbers
                value = value.replaceAll("[^\\d]", "");

                // Check what selector was used
                if (selector.equals(selectors[0])) {
                    // Check if the value is by night
                    String nightlyCheck = rental.select("span.bloc__ribbon--info--darken > span ~ span:contains(/noite)").text();
                    if (!nightlyCheck.isEmpty()) {
                        double newValue = Double.parseDouble(value);
                        value = String.valueOf(newValue * nightQuantity);
                    }
                }

                break;
            }

            // Stop the search if a non-valid value is found
            if (value.isEmpty()) {
                rentals.add(null);
                break;
            }

            // Skips the out of budget rentals
            double rentalTotalPrice = Double.parseDouble(value);
            if (rentalTotalPrice > maxPrice * maxPeople) continue;

            // Get the rental's URL
            String rentalUrl = rental.select("div.bloc__header__text > a").attr("href");

            // Skips the rentals without a viable distance
            double distance = getDistance(rentalUrl, driver);
            if (distance > MAX_DISTANCE) continue;

            // Get the rental's data
            String rentalName = rental.select("div.bloc__header__text > a").text();
            double rentalNightPrice = rentalTotalPrice / nightQuantity;

            // Adds the valid rental
            rentals.add(new Rental(rentalName, rentalUrl, distance, rentalNightPrice, rentalTotalPrice));
        }

        return rentals;
    }

    /**
     * Scrapes the rental information from MediaFerias website.
     *
     * @param driver The WebDriver instance to use.
     * @return A list of Rental objects representing the scraped rentals.
     */
    @Override
    public List<Rental> scrape(WebDriver driver) {
        if (driver == null) throw new IllegalArgumentException("The driver can´t be null!");

        // Hash set of all the valid rentals
        HashSet<Rental> rentals = new HashSet<>();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME));
        int pageNumber = 0;

        // Goes through all the pages in the search query
        while (true) {
            // Get the web page
            driver.get(getPage(pageNumber));

            // Wait until the page is loaded, and checks if there is content
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.property-bloc-autour")));
            } catch (TimeoutException e) {
                break; // Exit loop if the page content is not loaded in time
            }

            // Stores the page info
            Document searchDoc = Jsoup.parse(driver.getPageSource());

            // Adds the new rentals
            rentals.addAll(getRentals(searchDoc, driver));

            // Check if the search has stopped because of non-valid values
            if (rentals.contains(null)) {
                rentals.remove(null); // Remove the null from the hash map
                break;
            }

            // Prepare next page
            pageNumber++;
        }

        return new ArrayList<>(rentals);
    }
}
