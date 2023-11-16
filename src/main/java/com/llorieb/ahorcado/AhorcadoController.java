package com.llorieb.ahorcado;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.util.Duration;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class AhorcadoController {

    @FXML
    private ImageView dibujo;
    @FXML
    private ImageView reloj;
    @FXML
    private Label palabraText;
    @FXML
    private Label tiempoText;
    @FXML
    private MenuItem menuJugar;
    @FXML
    private Button btnJugar;
    @FXML
    private Button btnDetener;
    @FXML
    private GridPane gridBotones;

    final int tiempoMaximo = 120; // Tiempo en segundos
    private int tiempoRestante = tiempoMaximo;
    private Timeline timeline;
    private int segmento;
    private int contador;
    String palabraSecreta;
    StringBuilder palabraActual;

    private final List<ImagenReloj> imagenesReloj = new ArrayList<>();

    private final List<ImagenPersonaje> imagenesPersonaje = new ArrayList<>();


    public void initialize() {
        //InputStream stream = getClass().getResourceAsStream("/images/100.png");
        //imageView.setImage(new Image(stream));
        cargarImagenesReloj();
        tiempoText.setText(String.valueOf(tiempoRestante));

        // Inicializa el juego aquí
        // Carga las imágenes, la palabra secreta y configura la UI
    }

    private void cargarImagenesReloj() {
        // Supongamos que tienes archivos PNG llamados imagen1.png, imagen2.png, etc.
        for (int i = 0; i <= 12; i++) {
            String nombreImagen = "/images/sw" + i + ".png";
            InputStream input = getClass().getResourceAsStream(nombreImagen);

            Image imagen = new Image(input);
            imagenesReloj.add(new ImagenReloj(imagen));
        }
    }

    private void cargarImagenesPersonaje() {
        // Supongamos que tienes archivos PNG llamados imagen1.png, imagen2.png, etc.
        for (int i = 100; i <= 106; i++) {
            String nombreImagen = "" + i + ".png";
            InputStream input = getClass().getResourceAsStream(nombreImagen);

            Image imagen = new Image(input);
            imagenesPersonaje.add(new ImagenPersonaje(imagen));
        }
    }

    @FXML
    private void iniciarJuego() {
        palabraActual = new StringBuilder();

        ponerEstadoJuego();

        // Configurar la imagen del ahorcado inicial
        Image image = new Image(getClass().getResourceAsStream("images/100.png"));
        dibujo.setImage(image);

        tiempoRestante = tiempoMaximo;
        tiempoText.setText(String.valueOf(tiempoRestante));

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
                ponerEstadoDetenido();

                Platform.runLater(this::timeout);
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        palabraSecreta = obtenerPalabraSecreta();
        System.out.println(palabraSecreta);

        enmascararPalabraAdivinar();
        mostrarPalabraAdivinar();
    }

    @FXML
    private void detenerJuego(ActionEvent event) {
        // Detener el Timeline y realizar otras acciones de interrupción si es necesario
        detenerTemporizador();

        // Resto de la lógica para manejar la interrupción del juego
        ponerEstadoDetenido();
    }

    @FXML
    private void cerrarAplicacion(ActionEvent event) {
        // Realizar cualquier limpieza o acciones necesarias antes de cerrar la aplicación
        detenerTemporizador();

        // Cerrar la aplicación
        Platform.exit();
    }

    private void timeout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Tiempo agotado");
        alert.setHeaderText(null);
        alert.setContentText("¡Tiempo agotado! Perdiste");

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

    private String obtenerPalabraSecreta() {
        PalabraDAO palabraDAO = new PalabraDAO();
        return palabraDAO.seleccionarPalabraPorCategoria("Paises");
    }

    private void sonidoError() {
    }

    @FXML
    private void checkLetter(ActionEvent event) {
        Button button = (Button) event.getSource();
        char letra = button.getText().charAt(0);

        // Verificar si la letra está en la palabra secreta
        if (palabraSecreta.indexOf(letra) >= 0) {
            for (int i = 0; i < palabraSecreta.length(); i++) {
                char actual = palabraSecreta.charAt(i);
                if (actual == letra) {
                    palabraActual.setCharAt(i, letra);
                }
            }
            mostrarPalabraAdivinar();
        }
        else {
            sonidoError();
        }
    }

    private void mostrarPalabraAdivinar () {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < palabraActual.length(); i++) {
            builder.append(palabraActual.charAt(i)).append(' ');
        }

        palabraText.setText(builder.toString());
    }

    private void enmascararPalabraAdivinar() {
        for (char c : palabraSecreta.toCharArray()) {
            if (Character.isLetter(c)) {
                palabraActual.append("_");
            } else {
                palabraActual.append(" "); // Si no es una letra, espacio en blanco
            }
        }
    }

    private void ponerEstadoJuego() {
        btnJugar.setDisable(true);
        menuJugar.setDisable(true);
        btnDetener.setDisable(false);
        gridBotones.setDisable(false);
    }

    private void ponerEstadoDetenido() {
        btnJugar.setDisable(false);
        menuJugar.setDisable(false);
        btnDetener.setDisable(true);
        gridBotones.setDisable(true);
    }

    private void ponerEstadoInicialGraficos() {
        reloj.setImage(imagenesReloj.get(0).getImagen());
    }

    // Implementa aquí la lógica del juego, como verificar la letra elegida por el jugador,
    // actualizar la palabra actual, verificar si se ganó o se perdió, etc.
}