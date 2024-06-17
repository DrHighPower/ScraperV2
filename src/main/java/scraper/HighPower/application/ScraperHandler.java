package scraper.HighPower.application;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import scraper.HighPower.domain.Rental;
import scraper.HighPower.scraper.Airbnb;
import scraper.HighPower.scraper.MediaFerias;
import scraper.HighPower.scraper.Scraper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * The ScraperHandler class manages asynchronous scraping tasks for multiple Scraper objects.
 * It initializes WebDriver instances, performs scraping operations, and combines results
 * from different scrapers asynchronously using CompletableFuture.
 * <p>
 * This class supports concurrent scraping from multiple sources such as Airbnb and MediaFerias.
 * It uses Selenium WebDriver with ChromeDriver to simulate browser interaction and JSoup for
 * HTML parsing.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * ScraperHandler.run();
 * }
 * </pre>
 * </p>
 *
 * @see Scraper
 * @see Airbnb
 * @see MediaFerias
 */
public class ScraperHandler {

    /**
     * Asynchronously scrapes rental information from a Scraper object.
     *
     * @param scraper The Scraper object to scrape from.
     * @return A CompletableFuture holding a list of Rental objects scraped asynchronously.
     */
    private static CompletableFuture<List<Rental>> getRentalsAsync(Scraper scraper) {
        return CompletableFuture.supplyAsync(() -> {
            // Add settings to the driver
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");

            // Initiate the driver
            WebDriver driver = new ChromeDriver();

            // Perform the scraping task
            List<Rental> rentals = scraper.scrape(driver);

            // Close the driver
            driver.quit();

            return rentals;
        });
    }

    /**
     * Runs the scraper handler to scrape rental information from multiple sources concurrently.
     * It combines the results into a single list of Rental objects and prints them to the console.
     */
    public static void run() {

        // The list of scrapers. ToDo: Add a way to select which scrappers to use in the config file
        List<Scraper> scrapers = new ArrayList<>(List.of(new Airbnb(), new MediaFerias()));

        // Create a list of CompletableFuture for each scraper
        List<CompletableFuture<List<Rental>>> futures = new ArrayList<>();

        // Go through each scraper
        for (Scraper scraper : scrapers) {
            CompletableFuture<List<Rental>> future = getRentalsAsync(scraper);
            futures.add(future);
        }

        // Combine results once all tasks are completed
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[futures.size()])
        );
        CompletableFuture<List<Rental>> combinedFuture = allFutures.thenApply(
                v -> futures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(List::stream)
                        .toList()
        );

        try {
            // Get the combined results
            List<Rental> allRentals = combinedFuture.get();

            //ToDo: Change to output into an Excel file
            allRentals.forEach(System.out::println);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
