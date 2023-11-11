package com.llorieb.ahorcado;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.util.Duration;

import java.io.InputStream;

public class AhorcadoController {

    @FXML
    private ImageView dibujo;
    @FXML
    private Label palabraText;
    @FXML
    private Label tiempoText;
    private int tiempoRestante = 30; // Tiempo en segundos
    private Timeline timeline;

    private int segmento;

    private int contador;

    public void initialize() {
        //InputStream stream = getClass().getResourceAsStream("/images/100.png");
        //imageView.setImage(new Image(stream));

        tiempoText.setText(String.valueOf(tiempoRestante));

        // Inicializa el juego aquí
        // Carga las imágenes, la palabra secreta y configura la UI
    }

    @FXML
    private void iniciarJuego() {
        StringBuilder palabraActual;
        String palabraSecreta;

        // Configurar la imagen del ahorcado inicial
        Image image = new Image(getClass().getResourceAsStream("images/100.png"));
        dibujo.setImage(image);

        segmento = tiempoRestante / 12;
        contador = 0;

        // Configurar el temporizador
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            tiempoRestante--;
            tiempoText.setText(String.valueOf(tiempoRestante));

            contador++;
            if (contador == segmento) {
                contador = 0;
            }

            if (tiempoRestante <= 0) {
                detenerTemporizador();
                Platform.runLater(this::timeout);
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        palabraSecreta = obtenerPalabraSecreta();
        palabraActual = new StringBuilder("_".repeat(palabraSecreta.length()));
        palabraText.setText(palabraActual.toString());

        // Configurar la palabra oculta con guiones bajos
        StringBuilder palabraOculta = new StringBuilder();
        for (int i = 0; i < palabraSecreta.length(); i++) {
            palabraOculta.append("_ ");
        }
        palabraText.setText(palabraOculta.toString());
    }


    private String obtenerPalabraSecreta() {
        PalabraDAO palabraDAO = new PalabraDAO();
        return palabraDAO.seleccionarPalabraPorCategoria("Paises");
    }


    private void timeout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Tiempo agotado");
        alert.setHeaderText(null);
        alert.setContentText("¡Tiempo agotado! Usted ha perdido");

        // Establecer el cuadro de diálogo como modal para bloquear la interacción con la ventana principal
        alert.initModality(Modality.APPLICATION_MODAL);

        // Añadir botón "OK"
        alert.getButtonTypes().setAll(ButtonType.OK);

        ImageView imagen = new ImageView (new Image(getClass().getResourceAsStream("images/sw12.png")));
        imagen.setFitWidth(64);
        imagen.setFitHeight(64);
        alert.setGraphic(imagen);

        // Mostrar el cuadro de diálogo y esperar a que el usuario lo cierre
        alert.showAndWait();
    }

    private void detenerTemporizador() {
        if (timeline != null && timeline.getStatus() == Animation.Status.RUNNING) {
            timeline.stop();
        }
    }


    public void checkLetter() {
        // Implementa la lógica para verificar si la letra es correcta y actualizar la palabra oculta
    }

    // Implementa aquí la lógica del juego, como verificar la letra elegida por el jugador,
    // actualizar la palabra actual, verificar si se ganó o se perdió, etc.
}
