package scraper.HighPower.application;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.AbstractDriverOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@code WebDriverConfig} class provides methods to configure and retrieve
 * WebDriver options based on a properties file and system defaults.
 * <p>
 * It reads configuration from a properties file and determines the browser
 * to be used, as well as specific options for Chrome browser.
 * </p>
 * <p>
 * The properties file should be located at {@code src/main/resources/driver.properties}.
 * </p>
 */
public class WebDriverConfig {
    private static final String CONFIGURATION_FILENAME = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "driver.properties";

    /**
     * Retrieves properties from the configuration file.
     *
     * @return The properties loaded from the configuration file.
     */
    private static Properties getProperties() {
        Properties props = new Properties();

        // Read configured values
        try {
            InputStream in = new FileInputStream(CONFIGURATION_FILENAME);
            props.load(in);
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return props;
    }

    /**
     * Retrieves the default browser from the system registry.
     * This method is specific to Windows systems.
     *
     * <p>
     * Method from: <a href="https://stackoverflow.com/a/15852886">stackoverflow.com</a>
     * </p>
     *
     * @return The default browser as a String.
     */
    private static String getDefaultBrowser() {
        try {
            // Get registry where we find the default browser
            Process process = Runtime.getRuntime().exec("REG QUERY HKEY_CLASSES_ROOT\\http\\shell\\open\\command");
            Scanner kb = new Scanner(process.getInputStream());
            while (kb.hasNextLine()) {
                // Get output from the terminal, and replace all '\' with '/' (makes regex a bit more manageable)
                String registry = (kb.nextLine()).replaceAll("\\\\", "/").trim();

                // Extract the default browser
                Matcher matcher = Pattern.compile("/(?=[^/]*$)(.+?)[.]").matcher(registry);
                if (matcher.find()) {
                    // Scanner is no longer needed if match is found, so close it
                    kb.close();
                    String defaultBrowser = matcher.group(1);

                    // Capitalize first letter and return String
                    defaultBrowser = defaultBrowser.substring(0, 1).toUpperCase() + defaultBrowser.substring(1, defaultBrowser.length());
                    return defaultBrowser;
                }
            }
            // Match wasn't found, still need to close Scanner
            kb.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Have to return something if everything fails
        return "Error: Unable to get default browser";
    }

    /**
     * Retrieves the browser specified in the properties file or the default browser if not specified.
     *
     * @return The browser to be used for WebDriver.
     */
    public static String getBrowser() {
        return getProperties().getProperty("browser", getDefaultBrowser());
    }

    /**
     * Configures ChromeOptions based on the properties file.
     *
     * @param properties The properties loaded from the configuration file.
     * @return The configured ChromeOptions.
     */
    private static ChromeOptions getChromeOptions(Properties properties) {
        ChromeOptions options = new ChromeOptions();

        // Add logging preferences if enabled
        if (Boolean.parseBoolean(properties.getProperty("loggingPrefs.enabled", "false"))) {
            LoggingPreferences preferences = new LoggingPreferences();

            // Get the file info
            String logType = properties.getProperty("loggingPrefs.logType", "performance");
            Level logLevel = Level.parse(properties.getProperty("loggingPrefs.level", "ALL"));

            // Enable logging preferences
            preferences.enable(logType, logLevel);
            options.setCapability("goog:loggingPrefs", preferences);
        }

        // Set the page load strategy
        String pageLoadStrategy = properties.getProperty("options.pageLoadStrategy", "NORMAL");
        options.setPageLoadStrategy(PageLoadStrategy.valueOf(pageLoadStrategy));

        // Get all the arguments from the file
        String arguments = properties.getProperty("options.arguments", "");
        if (!arguments.isEmpty()) {
            for (String argument : arguments.split(",")) {
                options.addArguments(argument.trim());
            }
        }

        return options;
    }

    /**
     * Retrieves the driver options based on the browser type specified in the properties file.
     * Currently, only Chrome options are supported.
     *
     * @return The driver options for the specified browser.
     */
    public static AbstractDriverOptions getDriverOptions() {
        Properties properties = getProperties();

        // Get the browser used for scrapping
        String browserType = getBrowser();

        // Configure options based on browser type
        if ("chrome".equalsIgnoreCase(browserType)) {
            return getChromeOptions(properties);
        }
        // TODO: Add other browsers

        return null;
    }

}
