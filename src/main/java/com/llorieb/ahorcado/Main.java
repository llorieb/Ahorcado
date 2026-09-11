package com.llorieb.ahorcado;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/AhorcadoLayout.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        primaryStage.setTitle("Ahorcado");
        primaryStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/images/app-icon.png"))
        );
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Precalienta el motor de audio una vez que la ventana ya está visible.
        // Así, la inicialización del dispositivo no recorta el primer efecto real.
        AhorcadoController controller = loader.getController();
        controller.precalentarAudio();
    }

    @Override
    public void stop() {
        DatabaseConnection.closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
