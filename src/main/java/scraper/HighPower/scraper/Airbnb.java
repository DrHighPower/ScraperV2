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
import java.time.format.TextStyle;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * The Airbnb class is responsible for scraping rental property information
 * from Airbnb's website. It constructs search URLs based on user preferences
 * and parses the HTML content to extract rental details such as name, URL,
 * distance, price per night, and total price.
 * <p>
 * The class uses Selenium WebDriver to navigate and interact with Airbnb's
 * search pages and JSoup to parse the HTML content.
 * </p>
 *
 * <p>
 * It supports two types of searches:
 * <ul>
 * <li>Flexible dates</li>
 * <li>Specific date range</li>
 * </ul>
 * </p>
 *
 * <p>
 * The search parameters, including country, maximum number of people, maximum
 * price, and amenities, are initialized from the ApplicationSession class.
 * </p>
 *
 * <p>
 * The class also provides methods to extract rental coordinates from the Google
 * Maps link on the search page and calculate the distance from a specified point.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * Airbnb airbnb = new Airbnb();
 * WebDriver driver = new ChromeDriver();
 * List<Rental> rentals = airbnb.scrape(driver);
 * driver.quit();
 * }
 * </pre>
 * </p>
 *
 * @see Scrapable
 * @see ApplicationSession
 * @see CalculationUtils
 */
public class Airbnb implements Scrapable {
    private static final String URL = "https://www.airbnb.pt";
    private static final int WAIT_TIME = ApplicationSession.getWait();

    private static final double LATITUDE = ApplicationSession.getLatitude();
    private static final double LONGITUDE = ApplicationSession.getLongitude();
    private static final double MAX_DISTANCE = ApplicationSession.getMaximumDistance();
    private final String tripLength = "one_week";
    private final String pickerType;
    private final List<Integer> amenities = new ArrayList<>();
    private final String country;
    private final int maxPeople;
    private final int maxPrice;
    private final List<String> flexibleTripDates = new ArrayList<>();
    private final int nightQuantity;
    private String searchQuery;
    private List<LocalDate> tripDates;


    /**
     * Constructs an Airbnb object, initializing the search parameters from the ApplicationSession.
     */
    public Airbnb() {
        this.country = ApplicationSession.getCountry();
        this.maxPeople = ApplicationSession.getPeopleQuantity();
        this.maxPrice = ApplicationSession.getMaximumPrice();

        // Get the dates for the search
        LocalDate startDate = ApplicationSession.getStartDate();
        LocalDate endDate = ApplicationSession.getEndDate();

        // Get the type of search
        if (ApplicationSession.getFlexibility()) {
            this.pickerType = "flexible_dates";
            this.nightQuantity = 5;

            LocalDate currentDate = startDate.withDayOfMonth(1);
            LocalDate end = endDate.withDayOfMonth(1);

            // Go through all the months between the dates
            while (!currentDate.isAfter(end)) {
                flexibleTripDates.add(currentDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase());
                currentDate = currentDate.plusMonths(1);
            }
        } else {
            this.nightQuantity = (int) DAYS.between(startDate, endDate) - 1;
            this.pickerType = "calendar";
            tripDates = new ArrayList<>(List.of(startDate, endDate));
        }

        // Add amenities
        if (ApplicationSession.getPool()) amenities.add(7);
    }

    /**
     * Gets the CSS position of the rental element on the page.
     *
     * @param searchDoc  The document to search within.
     * @param rentalName The name of the rental property.
     * @return An array containing the left and top CSS positions.
     */
    private static double[] getPillPosition(Document searchDoc, String rentalName) {
        // Select the element by the text
        Elements elements = searchDoc.select("div[style~=touch-action: pan-x pan-y]:has(span:containsOwn(" + rentalName + "))");

        // Store the position
        double leftValue = 0;
        double topValue = 0;

        if (!elements.isEmpty()) {
            String divElement = elements.last().toString().split("\n")[0];

            // Regex matchers to get the values
            Matcher matcherLeft = Pattern.compile("left:\\s*(-?\\d+\\.\\d+)px;").matcher(divElement);
            Matcher matcherTop = Pattern.compile("top:\\s*(-?\\d+\\.\\d+)px;").matcher(divElement);

            try {
                if (matcherLeft.find()) leftValue = Double.parseDouble(matcherLeft.group(1));
                if (matcherTop.find()) topValue = Double.parseDouble(matcherTop.group(1));
            } catch (NullPointerException | NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return new double[]{leftValue, topValue};
    }

    /**
     * Creates the URL for the search query based on the specified parameters.
     */
    private void createURL() {
        // Creates the URL for the search
        StringBuilder builder = new StringBuilder(URL);
        builder.append("/s/homes?tab_id=home_tab")
                .append("&flexible_trip_lengths%5B%5D=").append(tripLength)
                .append("&query=").append(country)
                .append("&date_picker_type=").append(pickerType)
                .append("&adults=").append(maxPeople)
                .append("&price_max=").append(maxPrice); // Max price per night

        // Check the type of search
        if (pickerType.matches("flexible_dates")) {
            builder.append("&price_filter_num_nights=5");

            // Get the months for the flexible search
            for (String month : flexibleTripDates) {
                builder.append("&flexible_trip_dates%5B%5D=").append(month);
            }
        } else {

            // Get the night quantity to filter the rentals
            LocalDate startDate = tripDates.get(0);
            LocalDate endDate = tripDates.get(1);
            builder.append("&price_filter_num_nights=").append(DAYS.between(startDate, endDate) - 1)
                    .append("&checkin=").append(startDate)
                    .append("&checkout=").append(endDate);
        }

        // Add all amenities to the search query
        for (int amenity : amenities) {
            builder.append("&amenities%5B%5D=").append(amenity);
        }

        searchQuery = builder.toString();
    }

    /**
     * Gets the URL for a specific page of the search results.
     *
     * @param page The page number to get.
     * @return The URL for the specified page.
     */
    private String getPage(int page) {
        if (searchQuery == null) createURL();

        // Creates the base64 page select
        String cursorQuery = "{\"section_offset\":2,\"items_offset\":" + page + ",\"version\":1}";
        String b64Query = "&cursor=" + Base64.getEncoder().encodeToString(cursorQuery.getBytes());

        return searchQuery + b64Query;
    }

    /**
     * Extracts the coordinates and zoom level from the Google Maps link in the document.
     *
     * @param searchDoc The document to search within.
     * @return An array containing the latitude, longitude, and zoom level.
     */
    private double[] getMapCoordinates(Document searchDoc) {
        // Get the google maps link
        String mapsLink = searchDoc.select("a:has(img[alt='Google'])").attr("href");

        // Regex matchers to get the coordinates and zoom
        Matcher matcherCoords = Pattern.compile("[-]?[\\d]+[.][\\d]*,[-]?[\\d]+[.][\\d]*").matcher(mapsLink);
        Matcher matcherZoom = Pattern.compile("z=([\\d.]+)").matcher(mapsLink);

        // Store the coordinates and zoom
        double latitude = 0;
        double longitude = 0;
        double zoom = 0;

        try {
            // Get the coordinates
            if (matcherCoords.find()) {
                String[] linkCoords = matcherCoords.group(0).split(",", 0);
                latitude = Double.parseDouble(linkCoords[0]);
                longitude = Double.parseDouble(linkCoords[1]);
            }

            // Get the zoom
            if (matcherZoom.find()) {
                zoom = Double.parseDouble(matcherZoom.group(1));
            }
        } catch (NullPointerException | NumberFormatException e) {
            e.printStackTrace();
        }

        return new double[]{latitude, longitude, zoom};
    }

    /**
     * Extracts the rentals from the document.
     *
     * @param searchDoc The document to search within.
     * @return A set of Rental objects extracted from the document.
     */
    private HashSet<Rental> getRentals(Document searchDoc) {

        // Gets all the rental cards
        Elements searchedRentals = searchDoc.select("[data-testid='card-container']");

        // Stores the valid rentals
        HashSet<Rental> rentals = new HashSet<>();

        // Go through all the cards containing the rentals
        for (Element rental : searchedRentals) {
            // Get the rental's name
            String rentalName = rental.select("[data-testid='listing-card-title']").text();

            // Get the coordinates of the rental
            double[] pillPosition = getPillPosition(searchDoc, rentalName);
            double[] mapData = getMapCoordinates(searchDoc);
            double[] rentalCoordinates = CalculationUtils.cssToCoordinates(pillPosition[0], pillPosition[1], mapData[0], mapData[1], mapData[2]);

            // Get the distance of the rental
            double distance = CalculationUtils.haversine(LATITUDE, LONGITUDE, rentalCoordinates[0], rentalCoordinates[1]);

            // Check if it's a viable distance
            if (distance > MAX_DISTANCE) continue;

            // Get rental data
            String rentalUrl = URL + rental.select("a").attr("href");
            double rentalNightPrice = Double.parseDouble(rental.select("div[aria-hidden='true'] > span[class]").text().replaceAll("[^\\d.]", ""));
            double rentalTotalPrice = rentalNightPrice * nightQuantity;

            // Adds the valid rental
            rentals.add(new Rental(rentalName, rentalUrl, distance, rentalNightPrice, rentalTotalPrice));
        }

        return rentals;
    }

    /**
     * Scrapes rental properties from Airbnb using the provided WebDriver.
     *
     * @param driver The WebDriver to use for scraping.
     * @return A list of Rental objects representing the scraped rental properties.
     * @throws IllegalArgumentException If the provided driver is null.
     */
    @Override
    public List<Rental> scrape(WebDriver driver) {
        if (driver == null) throw new IllegalArgumentException("The driver can´t be null!");

        // Hash set of all the valid rentals
        HashSet<Rental> rentals = new HashSet<>();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME));
        boolean hasPage = true;
        int pageNumber = 0;

        // Goes through all the pages in the search query
        while (hasPage) {
            // Get the web page
            driver.get(getPage(pageNumber));

            // Wait until the page is loaded, and checks if there is content
            try {
                // Check if every rental have been loaded
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("meta[content='18']")));

                // Check if the map has been loaded
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("img[alt='Google']")));
            } catch (TimeoutException e) {
                hasPage = false; // Leaves the loop when it is in the last page
            }

            // Stores the page info
            Document searchDoc = Jsoup.parse(driver.getPageSource());

            // Check if the page has been repeated, if not it adds the new rentals
            if (!rentals.addAll(getRentals(searchDoc))) break;

            // Prepare next page
            pageNumber += 18;
        }

        return new ArrayList<>(rentals);
    }
}
