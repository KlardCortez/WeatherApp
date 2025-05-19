package org.example.appweather;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class GetAPI {
    private static final String API_key = "ca0ca2499ff7712218d42866b9e407ae";

    // New method: Pagkuha sa mga coordinate sa bisan aha nga lugar sa Philippines ra
    private static double[] getCoordinates(String location) {
        String formattedLocation = location.trim().replaceAll("\\s+", "+");
        String url = "http://api.openweathermap.org/geo/1.0/direct?q=" + formattedLocation + ",PH&limit=1&appid=" + API_key;

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONArray arr = new JSONArray(response.body());
                if (arr.length() > 0) {
                    JSONObject obj = arr.getJSONObject(0);
                    double lat = obj.getDouble("lat");
                    double lon = obj.getDouble("lon");
                    return new double[]{lat, lon};
                }
            }
        } catch (Exception e) {
            System.out.println("Geocoding API error: " + e.getMessage());
        }

        return null; // kung dili niya makita ang city or lugar
    }

    static int request(String locationName) {
        double[] coords = getCoordinates(locationName);
        if (coords == null) {
            System.out.println("Location not found.");
            return 1;
        }

        String url = String.format(Locale.ENGLISH,
                "https://api.openweathermap.org/data/2.5/weather?lat=%.6f&lon=%.6f&units=metric&appid=%s",
                coords[0], coords[1], API_key);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());

                JSONObject mainData = jsonResponse.getJSONObject("main");
                double temp = mainData.getDouble("temp");
                int humidity = mainData.getInt("humidity");

                JSONObject windData = jsonResponse.getJSONObject("wind");
                double windSpeed = windData.getDouble("speed");

                JSONArray weatherArray = jsonResponse.getJSONArray("weather");
                String weatherDescription = weatherArray.getJSONObject(0).getString("description");

                temp = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", temp));
                windSpeed = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", windSpeed));

                int timezoneOffsetSeconds = jsonResponse.getInt("timezone");
                Instant now = Instant.now().plusSeconds(timezoneOffsetSeconds);
                String date = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
                        .withLocale(Locale.ENGLISH)
                        .withZone(ZoneId.of("UTC"))
                        .format(now);

                Weather.getSearchHistory().add(new Weather(locationName, temp, humidity, weatherDescription, windSpeed, false, date));

                return 0;
            } else {
                JSONObject errorResponse = new JSONObject(response.body());
                String message = errorResponse.optString("message", "Unknown error");
                System.out.println("API error: " + message);
            }
        } catch (Exception e) {
            System.out.println("Weather API error: " + e.getMessage());
        }

        return 1;
    }
}
