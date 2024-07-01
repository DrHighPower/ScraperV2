package scraper.HighPower.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;
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
import java.util.Set;
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
 * @see Scraper
 * @see Rental
 * @see ApplicationSession
 * @see CalculationUtils
 */
public final class MediaFerias extends Scraper {
    private static final String URL = "https://www.mediaferias.com";
    private final int countryCode = ApplicationSession.getMediaFeriasCountryCode();
    private final int nightQuantity;
    private final int pool;
    private final LocalDate[] tripDates;

    /**
     * Constructs a MediaFerias object, initializing the search parameters from the {@link ApplicationSession}.
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
        if (locationCode < 0) return "00";
        return locationCode <= 9 ? "0" + locationCode : String.valueOf(locationCode);
    }

    /**
     * Creates the URL for the search query based on the specified parameters.
     */
    private void createURL() {
        // Creates the URL for the search
        searchQuery = URL + "/aluguer-ferias-" + COUNTRY.toLowerCase() +
                "/" + peopleQuantityToLetter(MAX_PEOPLE) +
                "00" + locationCodeToString(countryCode) +
                pool + "00/" +
                "?date1=" + tripDates[0] +
                "&date2=" + tripDates[1];
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
     * @throws RuntimeException If it wasn't possible to get the page.
     */
    private double getDistance(String url, WebDriver driver) {

        // Open the corresponding tab
        Object[] windowHandles = driver.getWindowHandles().toArray();
        driver.switchTo().window((String) windowHandles[1]);

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

        // Close the tab
        driver.close();

        // Go through the JavaScript
        for (Element script : jsScript) {
            // Regex matchers to get the coordinates
            Matcher latMatch = Pattern.compile("annonce_lat = +[-]?[\\d]+[.][\\d]*").matcher(script.data());
            Matcher lngMatch = Pattern.compile("annonce_lng = +[-]?[\\d]+[.][\\d]*").matcher(script.data());

            if (latMatch.find() && lngMatch.find()) {
                // Get the latitude and longitude
                double latitude = Double.parseDouble(latMatch.group().replaceAll("[^-\\d.]", ""));
                double longitude = Double.parseDouble(lngMatch.group().replaceAll("[^-\\d.]", ""));

                // Return the distance
                return CalculationUtils.haversine(LATITUDE, LONGITUDE, latitude, longitude);
            }
        }

        return 0;
    }

    /**
     * Retrieves the rental price from the rental card element.
     *
     * @param rentalCard The rental card element.
     * @return The rental price.
     */
    private double getPrice(Element rentalCard) {
        // The selectors to get the max price
        String[] selectors = {
                "span.bloc__ribbon--info--darken > span ~ span:not(.text--sm)",
                "span.bloc__ribbon--info--darken",
        };

        // Iterate over selectors to find the max price
        for (String selector : selectors) {
            String value = rentalCard.select(selector).text();

            // Continue to the next selector if no value is found
            if (value.isEmpty()) continue;

            // Extract only the numbers from the value
            value = value.replaceAll("[^\\d]", "");

            // Check what selector was used
            if (selector.equals(selectors[0])) {
                // Check if the value is by night
                String nightlyCheck = rentalCard.select("span.bloc__ribbon--info--darken > span ~ span:contains(/noite)").text();
                if (!nightlyCheck.isEmpty()) {
                    try {
                        double newValue = Double.parseDouble(value);
                        value = String.valueOf(newValue * nightQuantity);
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse nightly price: " + value);
                        return 0;
                    }
                }
            }

            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse price: " + value);
                return 0;
            }
        }

        return 0;
    }

    /**
     * Opens rental pages in new tabs.
     *
     * @param searchDoc The search result document.
     * @param driver    The WebDriver instance to use.
     */
    private void openRentals(Document searchDoc, WebDriver driver) {
        // Gets all the rental cards
        Elements searchedRentals = searchDoc.select("div.property-bloc-autour");

        // Go through all the cards containing the rentals
        for (Element rental : searchedRentals) {
            // Get the total price
            double rentalTotalPrice = getPrice(rental);

            // Stop the loop when non-valid rentals are reached
            if (rentalTotalPrice == 0) break;

            // Skips the out of budget rentals
            if (rentalTotalPrice > MAX_PRICE * MAX_PEOPLE) continue;

            // Get the rental's URL
            String rentalUrl = rental.select("div.bloc__header__text > a").attr("href");

            // Open a new tab for the rental
            driver.switchTo().newWindow(WindowType.TAB).get(rentalUrl);
        }
    }

    /**
     * Extracts the rentals from the document.
     *
     * @param searchDoc The document to search within.
     * @param driver    The WebDriver instance to use.
     * @return A set of {@link Rental} objects extracted from the document.
     */
    private Set<Rental> getRentals(Document searchDoc, WebDriver driver) {

        // Gets all the rental cards
        Elements searchedRentals = searchDoc.select("div.property-bloc-autour");

        // Stores the valid rentals
        Set<Rental> rentals = new HashSet<>();

        // Go through all the cards containing the rentals
        for (Element rental : searchedRentals) {
            // Get the total price
            double rentalTotalPrice = getPrice(rental);

            // Stop the search if a non-valid value is found
            if (rentalTotalPrice == 0) {
                rentals.add(null);
                break;
            }

            // Skips the out of budget rentals
            if (rentalTotalPrice > MAX_PRICE * MAX_PEOPLE) continue;

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
     * @return A list of {@link Rental} objects representing the scraped rentals.
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

            // Open all tabs for the rentals
            openRentals(searchDoc, driver);

            // Adds the new rentals
            rentals.addAll(getRentals(searchDoc, driver));

            // Go back to the search page
            driver.switchTo().window(driver.getWindowHandles().toArray(new String[0])[0]);

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
