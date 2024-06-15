package scraper.HighPower;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import scraper.HighPower.domain.Rental;
import scraper.HighPower.scraper.MediaFerias;

import java.util.List;

public class Main {
    public static void main(String[] args) {

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");

        WebDriver driver = new ChromeDriver();
        List<Rental> test = new MediaFerias().scrape(driver);
        driver.quit();

        System.out.println("Hello world!");
    }
}