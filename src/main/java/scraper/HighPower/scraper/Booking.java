package scraper.HighPower.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import scraper.HighPower.application.ApplicationSession;
import scraper.HighPower.application.CalculationUtils;
import scraper.HighPower.domain.Rental;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * The Booking class is responsible for scraping rental property information
 * from the Booking website. It constructs search URLs based on user preferences
 * and parses the HTML content to extract rental details such as name, URL, distance,
 * price per night, and total price.
 * <p>
 * The class uses Selenium WebDriver to navigate and interact with MediaFerias' search pages
 * and JSoup to parse the HTML content.
 * </p>
 * <p>
 * It handles both flexible and fixed trip dates and constructs the search URL accordingly.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * Booking booking = new Booking();
 * WebDriver driver = new ChromeDriver();
 * List<Rental> rentals = booking.scrape(driver);
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
public final class Booking extends Scraper {
    private static final int MAX_TABS = 10; // The maximum quantity of tabs to open to get the rental location
    private static final String URL = "https://www.booking.com";
    private final int nightQuantity;
    private final List<Integer> amenities = new ArrayList<>();
    private final List<String> flexibleTripDates = new ArrayList<>();
    private LocalDate[] tripDates;

    /**
     * Constructs a Booking object, initializing the search parameters from the {@link ApplicationSession}.
     */
    public Booking() {
        // Get the dates for the search
        LocalDate startDate = ApplicationSession.getStartDate();
        LocalDate endDate = ApplicationSession.getEndDate();

        // Get the night quantity
        if (ApplicationSession.getFlexibility()) {
            this.nightQuantity = ApplicationSession.getNightQuantity();

            DateTimeFormatter format = DateTimeFormatter.ofPattern("M-yyyy");
            LocalDate currentMonth = startDate.withDayOfMonth(1);

            // Go through all the months between the dates
            while (!currentMonth.isAfter(endDate)) {
                flexibleTripDates.add(currentMonth.format(format));

                currentMonth = currentMonth.plusMonths(1);
            }
        } else {
            this.nightQuantity = (int) DAYS.between(startDate, endDate) - 1;
            tripDates = new LocalDate[]{startDate, endDate};
        }

        // Add amenities
        if (ApplicationSession.getPool()) amenities.add(433);
    }

    /**
     * Creates the URL for the search based on the specified criteria.
     */
    private void createURL() {
        // Creates the URL for the search
        StringBuilder builder = new StringBuilder(URL);
        builder.append("/searchresults.en-gb.html?ss=").append(COUNTRY.replaceAll(" ", "%20")) //TODO: Change so it changes for the respective country (en-gb)
                .append("&group_adults=").append(MAX_PEOPLE);

        // Check the type of search
        if (!flexibleTripDates.isEmpty()) {
            builder.append("&ltfd=1%3A").append(nightQuantity).append("%3A")
                    .append(String.join("_", flexibleTripDates))
                    .append("%3A1%3A");
        } else {
            builder.append("&checkin=").append(tripDates[0])
                    .append("&checkout=").append(tripDates[1]);
        }

        // Make it search for houses and apartments
        builder.append("&nflt=ht_id%3D220%3Bht_id%3D201%3B");

        // Add the selected amenities
        for (int amenity : amenities) {
            builder.append("hotelfacility%3D").append(amenity).append("%3B");
        }

        // Max price per night
        builder.append("price%3DEUR-min-")
                .append((MAX_PRICE * MAX_PEOPLE) / nightQuantity)
                .append("-1");

        searchQuery = builder.toString();
    }

    /**
     * Retrieves the distance of a rental from a given location.
     *
     * @param url    The URL of the rental.
     * @param driver The WebDriver instance.
     * @return The distance of the rental in kilometers.
     */
    private double getDistance(String url, WebDriver driver) {

        // Open the corresponding tab
        Object[] windowHandles = driver.getWindowHandles().toArray();
        driver.switchTo().window((String) windowHandles[1]);

        // Load the page
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#hotel_header[data-atlas-latlng]")));
        } catch (TimeoutException e) {
            System.out.println("Map not found - " + url);
            return 0;
        }

        // Get the element with the coordinates
        Document rentalDoc = Jsoup.parse(driver.getPageSource());
        String coords = rentalDoc.select("#hotel_header[data-atlas-latlng]").attr("data-atlas-latlng");

        // Close the tab
        driver.close();

        // Regex matchers to get the coordinates
        Matcher matcherCoords = Pattern.compile("[-]?[\\d]+[.][\\d]*,[-]?[\\d]+[.][\\d]*").matcher(coords);

        try {
            // Get the coordinates and zoom
            if (matcherCoords.find()) {
                String[] linkCoords = matcherCoords.group(0).split(",", 0);
                double latitude = Double.parseDouble(linkCoords[0]);
                double longitude = Double.parseDouble(linkCoords[1]);

                // Return the distance
                return CalculationUtils.haversine(LATITUDE, LONGITUDE, latitude, longitude);
            }
        } catch (NullPointerException | NumberFormatException e) {
            System.out.println("Map not found - " + url);
        }

        return 0;
    }

    /**
     * Opens new tabs for each rental in the provided list.
     *
     * @param rentals The list of rental elements.
     * @param driver  The WebDriver instance.
     */
    private void openRentals(Elements rentals, WebDriver driver) {
        // Go through all the rentals
        for (Element rental : rentals) {
            // Get the rental URL to open the tab to
            String rentalUrl = rental.select("a[data-testid='title-link']").attr("href");

            // Open a new tab for the rental
            driver.switchTo().newWindow(WindowType.TAB).get(rentalUrl);
        }
    }

    /**
     * Retrieves rental information from the search results document.
     *
     * @param searchDoc The document containing the search results.
     * @param driver    The WebDriver instance.
     * @return A set of {@link Rental} objects containing rental information.
     */
    private Set<Rental> getRentals(Document searchDoc, WebDriver driver) {

        // Gets all the rental cards
        Elements searchedRentals = searchDoc.select("div[data-testid='property-card']");

        // Stores the valid rentals
        Set<Rental> rentals = new HashSet<>();

        for (int i = 0; i < searchedRentals.size(); i += MAX_TABS) {
            // Make a sublist of rentals to open
            Elements subList = new Elements(searchedRentals.subList(i, Math.min(i + MAX_TABS, searchedRentals.size())));

            // Open the tabs to get the rental locations
            openRentals(subList, driver);

            // Get the info from each rental
            for (Element rental : subList) {
                // Get the rental URL
                String rentalUrl = rental.select("a[data-testid='title-link']").attr("href");

                // Skips the rentals without a viable distance
                double distance = getDistance(rentalUrl, driver);
                if (distance > MAX_DISTANCE) continue;

                // Get rental info
                String totalPriceStr = rental.select("span[data-testid='price-and-discounted-price']").text().replaceAll("[^\\d]", "");
                ;
                double rentalTotalPrice = Double.parseDouble(totalPriceStr);
                double rentalNightPrice = rentalTotalPrice / nightQuantity;
                String rentalName = rental.select("div[data-testid='title']").text();

                // Add viable rental
                rentals.add(new Rental(rentalName, rentalUrl, distance, rentalNightPrice, rentalTotalPrice));
            }

            // Go back to the search page
            driver.switchTo().window(driver.getWindowHandles().toArray(new String[0])[0]);
        }

        return rentals;
    }

    /**
     * Scrapes the rental information from Booking website.
     *
     * @param driver The WebDriver instance to use.
     * @return A list of {@link Rental} objects representing the scraped rentals.
     */
    @Override
    public List<Rental> scrape(WebDriver driver) {
        if (driver == null) throw new IllegalArgumentException("The driver can´t be null!");

        // Get the web page
        createURL();

        // Open the search page
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME));
        driver.get(searchQuery);

        while (true) {

            // Go to the end of the page
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");

            try {
                // Check if button has been loaded
                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[@type='button'][span[contains(text(),'Load')]]")));
            } catch (TimeoutException e) {
                break; // Exit loop if the whole page has been loaded
            }

            // Load more content
            WebElement loadContent = driver.findElement(By.xpath("//button[@type='button'][span[contains(text(),'Load')]]"));
            loadContent.sendKeys(Keys.ENTER);
        }

        // Stores the page info
        Document searchDoc = Jsoup.parse(driver.getPageSource());

        return new ArrayList<>(getRentals(searchDoc, driver));
    }
}
