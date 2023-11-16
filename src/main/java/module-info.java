module com.llorieb.ahorcado {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.media;


    opens com.llorieb.ahorcado to javafx.fxml;
    exports com.llorieb.ahorcado;
}