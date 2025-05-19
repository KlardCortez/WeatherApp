package org.example.appweather;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

public class HelloApplication extends Application {

    TextField tf_city;
    Label lb_temperatureValue = new Label();
    Label lb_windSpeedValue = new Label();
    Label lb_humidityValue = new Label();
    Label lb_weatherStateValue = new Label();

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        Image logoImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/weather.png")));
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/weather.png")));
        stage.getIcons().add(icon);
        ImageView logoImageView = new ImageView(logoImage);
        logoImageView.setFitWidth(50);
        logoImageView.setFitHeight(50);

        Label lb_text = new Label("Weather App");
        lb_text.getStyleClass().add("label-title");

        HBox logoTitleBox = new HBox(10, logoImageView, lb_text);
        logoTitleBox.setAlignment(Pos.CENTER);
        logoTitleBox.setPadding(new Insets(10, 0, 0, 0));

        Label lb_city = new Label("Enter city name:");
        lb_city.getStyleClass().add("label");

        tf_city = new TextField();
        tf_city.setPromptText("Enter city name");
        tf_city.setPrefWidth(200);
        tf_city.setMaxWidth(250);
        tf_city.getStyleClass().add("text-field");

        Button btn_search = new Button("Search");
        btn_search.setPrefWidth(100);
        btn_search.getStyleClass().add("button");
        btn_search.setId("btn_search");
        btn_search.setOnAction(e -> searchBTN());

        VBox searchBox = new VBox(8, lb_city, tf_city, btn_search);
        searchBox.setAlignment(Pos.CENTER);

        // Weather Display ni mga choy
        Label lb_temperature = new Label("Temperature:");
        Label lb_humidity = new Label("Humidity:");
        Label lb_windSpeed = new Label("Wind Speed:");
        Label lb_weatherState = new Label("Weather State:");

        for (Label label : new Label[]{lb_temperature, lb_humidity, lb_windSpeed, lb_weatherState,
                lb_temperatureValue, lb_humidityValue, lb_windSpeedValue, lb_weatherStateValue}) {
            label.getStyleClass().add("label");
        }

        VBox weatherDetails = new VBox(10,
                new HBox(10, lb_temperature, lb_temperatureValue),
                new HBox(10, lb_humidity, lb_humidityValue),
                new HBox(10, lb_windSpeed, lb_windSpeedValue),
                new HBox(10, lb_weatherState, lb_weatherStateValue)
        );
        weatherDetails.setAlignment(Pos.CENTER);

        Button btn_history = new Button("History Search");
        btn_history.setPrefWidth(150);
        btn_history.getStyleClass().add("button");
        btn_history.setId("btn_history");
        btn_history.setOnAction(e -> historyBTN());

        Label poweredBy = new Label("Powered by OpenWeather API");
        poweredBy.setFont(new Font(10));
        poweredBy.setTextFill(Color.GRAY);

        VBox root = new VBox(20, logoTitleBox, searchBox, weatherDetails, btn_history, poweredBy);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(15));
        root.getStyleClass().add("root");

        Scene scene = new Scene(root, 360, 640);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/style.css")).toExternalForm());

        stage.setTitle("Weather App");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public void searchBTN() {
        String city = tf_city.getText().trim();

        if (city.isEmpty()) {
            showAlert("Input Error", "City name cannot be empty");
            return;
        }

        // Kung naay internet or wala
        if (!isInternetAvailable()) {
            networkError();
            return;
        }

        // Pag-validate sa ngalan sa City mga pangalan sa city og spaces ra pwede
        if (!isAlpha(city)) {
            nameError(city);
            return;
        }

        // loading ni siya pag search sa city
        tf_city.setDisable(true);
        lb_temperatureValue.setText("Loading...");
        lb_humidityValue.setText("Loading...");
        lb_windSpeedValue.setText("Loading...");
        lb_weatherStateValue.setText("Loading...");

        // Run API call in a background thread to avoid freezing UI
        new Thread(() -> {
            int result = GetAPI.request(city);

            // Pag-update sa UI balik sa Javafx application thread
            javafx.application.Platform.runLater(() -> {
                tf_city.setDisable(false);

                if (result == 0 && !Weather.getSearchHistory().isEmpty()) {
                    Weather latest = Weather.getSearchHistory().get(Weather.getSearchHistory().size() - 1);

                    lb_humidityValue.setText(latest.getHumidity() + " %");
                    lb_temperatureValue.setText(String.format(Locale.ENGLISH, "%.2f °C", latest.getTemp()));
                    lb_weatherStateValue.setText(capitalizeWords(latest.getState()));
                    lb_windSpeedValue.setText(String.format(Locale.ENGLISH, "%.2f m/s", latest.getWindSpeed()));
                } else {
                    requestError();
                    lb_temperatureValue.setText("-");
                    lb_humidityValue.setText("-");
                    lb_windSpeedValue.setText("-");
                    lb_weatherStateValue.setText("-");
                }
            });
        }).start();
    }

    //  capitalize tanan discription sa weather
    private String capitalizeWords(String input) {
        String[] words = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }



    public void historyBTN() {
        TableColumn<Weather, String> date = new TableColumn<>("Date");
        date.setCellValueFactory(new PropertyValueFactory<>("date"));
        date.setPrefWidth(100);

        TableColumn<Weather, String> city = new TableColumn<>("City");
        city.setCellValueFactory(new PropertyValueFactory<>("city"));
        city.setPrefWidth(100);

        TableColumn<Weather, Double> temp = new TableColumn<>("Temperature");
        temp.setCellValueFactory(new PropertyValueFactory<>("temp"));
        temp.setPrefWidth(100);

        TableColumn<Weather, Double> windSpeed = new TableColumn<>("Wind Speed");
        windSpeed.setCellValueFactory(new PropertyValueFactory<>("windSpeed"));
        windSpeed.setPrefWidth(100);

        TableColumn<Weather, Double> humidity = new TableColumn<>("Humidity");
        humidity.setCellValueFactory(new PropertyValueFactory<>("humidity"));
        humidity.setPrefWidth(100);

        TableColumn<Weather, String> state = new TableColumn<>("State");
        state.setCellValueFactory(new PropertyValueFactory<>("state"));
        state.setPrefWidth(100);

        TableView<Weather> table = new TableView<>();
        table.setMaxWidth(700);
        table.setMaxHeight(800);
        table.getColumns().addAll(date, city, temp, windSpeed, humidity, state);
        table.setItems(Weather.getSearchHistory());

        VBox layout = new VBox(table);
        layout.setStyle(" -fx-background-color: linear-gradient(to bottom, #a1c4fd, #c2e9fb);"); // Same background color as main scene
        layout.setPadding(new Insets(10));
        layout.setAlignment(Pos.CENTER);

        Scene archivesScene = new Scene(layout, 720, 500);
        archivesScene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm()); // Apply the CSS

        Stage stage = new Stage();
        stage.setTitle("Archives");
        stage.setScene(archivesScene);
        stage.setResizable(false);
        stage.show();
    }

    public boolean isInternetAvailable() {
        try {
            java.net.URL url = new java.net.URL("http://www.google.com");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000); // 3 seconds timeout
            connection.connect();
            return (connection.getResponseCode() == 200);
        } catch (IOException e) {
            return false;
        }
    }

    public void networkError() {
        Label lb_message = new Label("No network connection");
        lb_message.setFont(new Font("Times New Roman", 20));
        lb_message.setTextFill(Color.RED);
        lb_message.setAlignment(Pos.CENTER);

        Scene errorScene = new Scene(lb_message, 400, 150);
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(errorScene);
        stage.setTitle("Network Error");
        stage.show();
    }



    public void requestError() {
        Label lb_message = new Label("Server Error: city not found");
        lb_message.setFont(new Font("Times New Roman", 20));
        lb_message.setTextFill(Color.RED);
        lb_message.setAlignment(Pos.CENTER);

        Scene requestErrorScene = new Scene(lb_message, 400, 150);
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(requestErrorScene);
        stage.setTitle("Server Error");
        stage.show();
    }

    public void nameError(String cityName) {
        Label lb_message = new Label("Invalid City Name");
        lb_message.setFont(new Font("Times New Roman", 20));
        lb_message.setTextFill(Color.RED);
        lb_message.setAlignment(Pos.CENTER);

        Scene nameErrorScene = new Scene(lb_message, 400, 150);
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(nameErrorScene);
        stage.setTitle("Invalid Name");
        stage.show();
    }

    public boolean isAlpha(String n) {
        for (int i = 0; i < n.length(); i++) {
            if (n.charAt(i) == ' ') continue;
            if (!Character.isLetter(n.charAt(i))) return false;
        }
        return true;
    }
}
