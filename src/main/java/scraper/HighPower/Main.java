package scraper.HighPower;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import scraper.HighPower.domain.Rental;
import scraper.HighPower.scraper.Airbnb;

import java.util.List;

public class Main {
    public static void main(String[] args) {

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");

        WebDriver driver = new ChromeDriver(options);
        List<Rental> test = new Airbnb().scrape(driver);
        driver.quit();

        System.out.println("Hello world!");
    }
}