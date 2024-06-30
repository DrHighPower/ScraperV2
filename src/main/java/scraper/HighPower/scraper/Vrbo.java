package scraper.HighPower.scraper;

import com.google.common.collect.ImmutableMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
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
import java.util.logging.Level;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * The {@code Vrbo} class is responsible for scraping rental data from the Vrbo website.
 * It extends the abstract {@link Scraper} class and implements the specific scraping
 * logic for Vrbo.
 *
 * <p>This class constructs the search URL based on user preferences, processes network
 * logs to extract rental information, and provides the results as a list of {@link Rental}
 * objects.</p>
 *
 * <p>Usage example:
 * <pre>
 *     LoggingPreferences preferences = new LoggingPreferences();
 *     preferences.enable(LogType.PERFORMANCE, Level.ALL);
 *
 *     ChromeOptions option = new ChromeOptions();
 *     option.setCapability("goog:loggingPrefs", preferences);
 *
 *     WebDriver driver = new ChromeDriver(option);
 *     Vrbo vrboScraper = new Vrbo();
 *     List<Rental> rentals = vrboScraper.scrape(driver);
 * </pre>
 * </p>
 *
 * <p>Note: This class is final and cannot be subclassed.</p>
 *
 * @see Scraper
 * @see Rental
 * @see ApplicationSession
 * @see CalculationUtils
 */
public final class Vrbo extends Scraper {
    private static final String URL = "https://www.vrbo.com";
    private static final int ATTEMPTS = 5;
    private final int countryCode = ApplicationSession.getVrboCountryCode();
    private final String tripLength = "1_WEEK";
    private final List<String> amenities = new ArrayList<>();
    private final List<String> flexibleTripDates = new ArrayList<>();
    private final int nightQuantity;
    private LocalDate[] tripDates;

    /**
     * Constructs a new Vrbo scraper and initializes the search parameters
     * based on user preferences stored in {@link ApplicationSession}.
     */
    public Vrbo() {

        // Get the dates for the search
        LocalDate startDate = ApplicationSession.getStartDate();
        LocalDate endDate = ApplicationSession.getEndDate();

        // Get the type of search
        if (ApplicationSession.getFlexibility()) {
            this.nightQuantity = ApplicationSession.getNightQuantity();

            LocalDate currentMonthStart = startDate.withDayOfMonth(1);

            // Go through all the months between the dates
            while (!currentMonthStart.isAfter(endDate)) {
                LocalDate currentMonthEnd = currentMonthStart.withDayOfMonth(currentMonthStart.lengthOfMonth());

                flexibleTripDates.add(currentMonthStart + "_" + currentMonthEnd);

                currentMonthStart = currentMonthStart.plusMonths(1);
            }
        } else {
            this.nightQuantity = (int) DAYS.between(startDate, endDate) - 1;
            tripDates = new LocalDate[]{startDate, endDate};
        }

        // Add amenities
        if (ApplicationSession.getPool()) amenities.add("pool");
    }

    /**
     * Creates the search URL based on the initialized parameters.
     */
    private void createURL() {
        // Creates the URL for the search
        StringBuilder builder = new StringBuilder(URL);
        builder.append("/pt-pt/search?") //TODO: Change so it changes for the respective country
                .append("regionId=").append(countryCode)
                .append("&adults=").append(MAX_PEOPLE)
                .append("&allowPreAppliedFilters=false")
                .append("&total_price=0%2C").append(MAX_PRICE * MAX_PEOPLE);

        // Check the type of search
        if (!flexibleTripDates.isEmpty()) {
            builder.append("&flexibility=").append(tripLength);

            // Get the months for the flexible search
            for (String month : flexibleTripDates) {
                builder.append("&searchRange=").append(month);
            }
        } else {
            builder.append("&startDate=").append(tripDates[0])
                    .append("&endDate=").append(tripDates[1]);
        }

        // Add all amenities to the search query
        if (!amenities.isEmpty()) {
            builder.append("&amenities_facilities_group=");

            for (String amenity : amenities) {
                builder.append(amenity).append("%2C");
            }
        }

        searchQuery = builder.toString();
    }

    /**
     * Processes a network log entry and extracts the JSON response body if it is a valid
     * Vrbo API response.
     *
     * @param entry  The log entry to process.
     * @param driver The WebDriver instance used for extracting the response body.
     * @return The JSON response body if the log entry is valid, null otherwise.
     */
    private JSONObject processLogEntry(LogEntry entry, WebDriver driver) {
        try {
            // Parse the log entry message as JSON
            JSONObject messageJson = new JSONObject(entry.getMessage());
            JSONObject message = messageJson.getJSONObject("message");

            // Continue to the next entry if the log is not a network response received event
            if (!"Network.responseReceived".equals(message.getString("method"))) {
                return null;
            }

            // Extract the params and response objects from the log message
            JSONObject params = message.getJSONObject("params");
            JSONObject response = params.getJSONObject("response");

            // Continue to the next entry if the response is not a JSON or does not contain "graphql" in the URL
            if (!response.getString("mimeType").contains("json") || !response.getString("url").contains("graphql")) {
                return null;
            }

            // Get the request ID from the params
            String requestId = params.getString("requestId");

            // Execute CDP command to get the response body using the request ID
            JSONObject responseBody = new JSONObject(
                    ((ChromeDriver) driver).executeCdpCommand("Network.getResponseBody",
                            ImmutableMap.of("requestId", requestId))
            );

            // Parse the response body as JSON
            return new JSONObject(responseBody.getString("body"));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Retrieves performance logs from the WebDriver. Attempts to fetch logs multiple times
     * if they are initially empty.
     *
     * @param driver The WebDriver instance to retrieve logs from.
     * @return The retrieved log entries.
     */
    private LogEntries getPerformanceLogs(WebDriver driver) {
        int attempts = ATTEMPTS;
        while (attempts > 0) {
            LogEntries logs = driver.manage().logs().get(LogType.PERFORMANCE);
            if (!logs.getAll().isEmpty()) {
                return logs;
            }
            try {
                Thread.sleep(2000); // Wait for 2 seconds before retrying
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            attempts--;
        }
        return driver.manage().logs().get(LogType.PERFORMANCE); // Return even if empty after retries
    }

    /**
     * Restarts the WebDriver instance, clearing cookies and reinitializing it with logging
     * preferences for capturing performance logs.
     *
     * @param driver The current WebDriver instance to restart.
     * @return A new WebDriver instance with logging preferences enabled.
     */
    public WebDriver restartDriver(WebDriver driver) {
        if (driver instanceof ChromeDriver) {
            driver.manage().deleteAllCookies();
            driver.quit();

            // Add the option to check the logs
            LoggingPreferences preferences = new LoggingPreferences();
            preferences.enable(LogType.PERFORMANCE, Level.ALL);
            ChromeOptions option = new ChromeOptions();
            option.setCapability("goog:loggingPrefs", preferences);

            // Make a new instance of the driver
            driver = new ChromeDriver(option);
        }// TODO: Add other browsers

        return driver;
    }

    /**
     * Retrieves network logs from the WebDriver and processes them to extract relevant
     * JSON responses.
     *
     * @param driver The WebDriver instance to retrieve logs from.
     * @return A list of JSON objects representing the network responses.
     */
    private List<JSONObject> getNetworkLogs(WebDriver driver) {
        // Get all the logs from the WebDriver
        LogEntries logs = getPerformanceLogs(driver);
        List<LogEntry> logList = logs.getAll();

        // Filter and process logs in parallel
        return logList.parallelStream()
                .map(entry -> processLogEntry(entry, driver))
                .filter(log -> log != null)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a JSON object representing a marker by its ID from a JSON array of markers.
     *
     * @param markers The JSON array of markers.
     * @param id      The ID of the marker to retrieve.
     * @return The JSON object representing the marker, or null if not found.
     */
    private JSONObject getJsonMarkerById(JSONArray markers, int id) {
        for (int i = 0; i < markers.length(); i++) {
            JSONObject marker = markers.getJSONObject(i);
            if (marker.getInt("id") == id) return marker;
        }

        return null;
    }

    /**
     * Extracts rental information from the network logs and returns a set of valid {@link Rental} objects.
     *
     * @param logs The network logs to process.
     * @return A set of valid rentals extracted from the logs.
     */
    private HashSet<Rental> getRentals(List<JSONObject> logs) {
        // Stores the valid rentals
        HashSet<Rental> rentals = new HashSet<>();

        for (JSONObject json : logs) {
            JSONObject propertySearch = json.getJSONObject("data").getJSONObject("propertySearch");
            JSONArray propertySearchListings = propertySearch.getJSONArray("propertySearchListings");
            JSONArray markers = propertySearch.getJSONObject("dynamicMap").getJSONObject("map").getJSONArray("markers");

            for (int i = 0; i < propertySearchListings.length(); i++) {

                JSONObject jsonRental;
                JSONObject jsonMap;
                try {
                    jsonRental = propertySearchListings.getJSONObject(i);
                    jsonMap = getJsonMarkerById(markers, jsonRental.getInt("id"));
                } catch (JSONException e) {
                    continue;
                }
                if (jsonMap == null) continue;

                // Get the distance
                double latitude = jsonMap.getJSONObject("markerPosition").getDouble("latitude");
                double longitude = jsonMap.getJSONObject("markerPosition").getDouble("longitude");

                double distance = CalculationUtils.haversine(LATITUDE, LONGITUDE, latitude, longitude);

                // Check if it's a viable distance
                if (distance > MAX_DISTANCE) continue;

                // Get rental's data
                String rentalName = jsonRental.getJSONObject("headingSection").getString("heading");
                String rentalUrl = URL + jsonRental.getJSONObject("cardLink").getJSONObject("resource").getString("relativePath");

                // Get rental price
                String rentalNightPriceStr = jsonRental.getJSONObject("priceSection")
                        .getJSONObject("priceSummary")
                        .getJSONArray("displayMessages")
                        .getJSONObject(2).getJSONArray("lineItems")
                        .getJSONObject(0).getString("value");
                double rentalNightPrice = Double.parseDouble(rentalNightPriceStr.replaceAll("[^\\d.]", ""));
                double rentalTotalPrice = rentalNightPrice * nightQuantity;

                // Adds the valid rental
                rentals.add(new Rental(rentalName, rentalUrl, distance, rentalNightPrice, rentalTotalPrice));
            }
        }

        return rentals;
    }

    /**
     * Scrapes rental properties from Vrbo using the provided WebDriver.
     *
     * @param driver The WebDriver instance to use for scraping.
     * @return A list of {@link Rental} objects representing the scraped rentals.
     * @throws IllegalArgumentException If the provided WebDriver instance is null.
     */
    @Override
    public List<Rental> scrape(WebDriver driver) {
        if (driver == null) throw new IllegalArgumentException("The driver can´t be null!");

        // Stores the old driver
        WebDriver oldDriver = driver;

        // Get the web page
        createURL();

        // Loop until the network logs are loaded
        List<JSONObject> logs;
        do {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME));
            driver.get(searchQuery);

            // Go through all pages
            while (true) {
                // Wait until the page is loaded, and checks if there is content
                try {
                    // Check if the map has been loaded
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("img[alt='Google']")));

                    //Check if the next page is ready
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("button[data-stid='next-button']:not(button[disabled])")));
                } catch (TimeoutException e) {
                    break;
                }

                // Change page
                WebElement nextPage = driver.findElement(By.cssSelector("button[data-stid='next-button']:not(button[disabled])"));
                nextPage.sendKeys(Keys.ENTER);
            }

            // Get all logs with the rentals
            logs = getNetworkLogs(driver);

            if (logs.isEmpty()) driver = restartDriver(driver);
        } while (logs.isEmpty());

        // Check if the driver has changed
        if (!oldDriver.equals(driver)) driver.quit();

        // Return all viable rentals
        return new ArrayList<>(getRentals(logs));
    }
}
