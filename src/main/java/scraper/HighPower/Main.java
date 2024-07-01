package scraper.HighPower;

import scraper.HighPower.application.ScraperHandler;

public class Main {
    public static void main(String[] args) {
        ScraperHandler.run();

        System.out.println("The scrapping was successful!");
    }
}