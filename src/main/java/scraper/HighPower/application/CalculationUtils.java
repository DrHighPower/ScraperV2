package scraper.HighPower.application;

/**
 * The CalculationUtils class provides utility methods for performing various calculations.
 */
public class CalculationUtils {
    private static final int TILE_SIZE = 256; // Tile size for the coordinate calculations

    /**
     * Calculates the Haversine distance between two points on the Earth specified by latitude and longitude.
     *
     * @param lat1 Latitude of the first point.
     * @param lon1 Longitude of the first point.
     * @param lat2 Latitude of the second point.
     * @param lon2 Longitude of the second point.
     * @return The Haversine distance between the two points in kilometers.
     */
    public static double haversine(double lat1, double lon1,
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

    /**
     * Converts CSS pixel positions to geographic coordinates (latitude and longitude).
     *
     * @param left            The left pixel position relative to the center.
     * @param top             The top pixel position relative to the center.
     * @param centerLatitude  The latitude of the center point.
     * @param centerLongitude The longitude of the center point.
     * @param zoom            The zoom level of the map.
     * @return An array containing the latitude and longitude corresponding to the given CSS pixel positions.
     */
    public static double[] cssToCoordinates(double left, double top, double centerLatitude, double centerLongitude, double zoom) {
        // Convert center coordinates to tile numbers
        double centerTileX = (centerLongitude + 180) / 360 * Math.pow(2, zoom);
        double centerTileY = (1 - Math.log(Math.tan(Math.toRadians(centerLatitude)) + 1 / Math.cos(Math.toRadians(centerLatitude))) / Math.PI) / 2 * Math.pow(2, zoom);

        // Convert tile numbers to pixel positions
        double centerPixelX = centerTileX * TILE_SIZE;
        double centerPixelY = centerTileY * TILE_SIZE;

        // Calculate pill's pixel position
        double pillPixelX = centerPixelX + left;
        double pillPixelY = centerPixelY + top;

        // Convert pixel position back to tile numbers
        double pillTileX = pillPixelX / TILE_SIZE;
        double pillTileY = pillPixelY / TILE_SIZE;

        // Convert tile numbers back to coordinates
        double pillLongitude = pillTileX / Math.pow(2, zoom) * 360 - 180;
        double pillLatitude = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * pillTileY / Math.pow(2, zoom)))));

        return new double[]{pillLatitude, pillLongitude};
    }
}
