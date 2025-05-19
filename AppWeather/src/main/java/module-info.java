module org.example.appweather {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires org.json;


    opens org.example.appweather to javafx.fxml;
    exports org.example.appweather;
}