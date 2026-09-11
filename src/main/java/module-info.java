module com.llorieb.ahorcado {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;
    requires java.prefs;
    requires org.xerial.sqlitejdbc;

    opens com.llorieb.ahorcado to javafx.fxml;
    exports com.llorieb.ahorcado;
}
