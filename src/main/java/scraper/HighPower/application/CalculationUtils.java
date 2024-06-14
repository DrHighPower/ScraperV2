package scraper.HighPower.application;

/**
 * The CalculationUtils class provides utility methods for performing various calculations.
 */
public class CalculationUtils {

    /**
     * Calculates the Haversine distance between two points on the Earth specified by latitude and longitude.
     *
     * @param lat1 Latitude of the first point.
     * @param lon1 Longitude of the first point.
     * @param lat2 Latitude of the second point.
     * @param lon2 Longitude of the second point.
     * @return The Haversine distance between the two points in kilometers.
     */
    private static double haversine(double lat1, double lon1,
                                    double lat2, double lon2) {
        // Distance between latitudes and longitudes
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // Convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Apply formulae
        double a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.pow(Math.sin(dLon / 2), 2) *
                        Math.cos(lat1) *
                        Math.cos(lat2);
        double rad = 6371;
        double c = 2 * Math.asin(Math.sqrt(a));
        return rad * c;
    }
}
