module com.llorieb.ahorcado {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.llorieb.ahorcado to javafx.fxml;
    exports com.llorieb.ahorcado;
}