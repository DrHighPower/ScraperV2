package scraper.HighPower;

import scraper.HighPower.application.ApplicationSession;

public class Main {
    public static void main(String[] args) {
        boolean b = ApplicationSession.getPool();
        System.out.println("Hello world!");
    }
}