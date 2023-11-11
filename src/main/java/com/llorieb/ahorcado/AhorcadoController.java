package com.llorieb.ahorcado;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.io.InputStream;

public class AhorcadoController {

    @FXML
    private ImageView imageView;
    @FXML
    private Label palabraText;
    @FXML
    private Label tiempoText;
    private int tiempoRestante = 60; // Tiempo en segundos
    private Timeline timeline;

    public void initialize() {
        //InputStream stream = getClass().getResourceAsStream("/images/100.png");
        //imageView.setImage(new Image(stream));

        // Inicializa el juego aquí
        // Carga las imágenes, la palabra secreta y configura la UI
    }

    private void iniciarJuego() {
        StringBuilder palabraActual;
        String palabraSecreta;

        // Configurar la imagen del ahorcado inicial
        imageView.setImage(new Image(getClass().getResourceAsStream("images/100.svg")));

        // Configurar el temporizador
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            tiempoRestante--;
            tiempoText.setText(String.valueOf(tiempoRestante));
            if (tiempoRestante <= 0) {
                // El jugador ha perdido, implementa la lógica aquí
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

    public void checkLetter() {
        // Implementa la lógica para verificar si la letra es correcta y actualizar la palabra oculta
    }

    // Implementa aquí la lógica del juego, como verificar la letra elegida por el jugador,
    // actualizar la palabra actual, verificar si se ganó o se perdió, etc.
}
