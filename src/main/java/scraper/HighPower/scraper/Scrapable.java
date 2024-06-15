package scraper.HighPower.scraper;

import org.openqa.selenium.WebDriver;
import scraper.HighPower.domain.Rental;

import java.util.List;

/**
 * The Scrapable interface defines a method for scraping rental data.
 */
public interface Scrapable {

    /**
     * Scrapes rental data and returns a list of Rental objects.
     *
     * @return A list of Rental objects containing the scraped rental data.
     */
    List<Rental> scrape(WebDriver driver);
}
