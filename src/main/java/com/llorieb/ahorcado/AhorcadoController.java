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
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;


public class AhorcadoController {

    @FXML
    private ImageView dibujo;
    @FXML
    private Rectangle fondoDibujo;
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
    private Button btnArriesgar;
    @FXML
    private GridPane gridBotones;

    final int tiempoMaximo = 12; // Tiempo en segundos
    private int tiempoRestante = tiempoMaximo;
    private Timeline timeline;
    private int segmentoTiempo;
    private int contadorTiempo;
    private int indiceReloj;
    private int indicePersonaje;
    private String palabraSecreta;
    private StringBuilder palabraActual;
    private ResourceBundle bundle;
    TextInputDialog dialogArriesgar;
    private final List<ImagenReloj> imagenesReloj = new ArrayList<>();
    private final List<ImagenPersonaje> imagenesPersonaje = new ArrayList<>();
    private final int MAX_INTENTOS = 6;

    public void initialize() {
        bundle = ResourceBundle.getBundle("/properties/textos_es");

        cargarImagenesReloj();
        cargarImagenesPersonaje();

        tiempoText.setText(String.valueOf(tiempoRestante));
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
            String nombreImagen = "/images/" + i + ".png";
            InputStream input = getClass().getResourceAsStream(nombreImagen);

            Image imagen = new Image(input);
            imagenesPersonaje.add(new ImagenPersonaje(imagen));
        }
    }

    @FXML
    private void iniciarJuego() {
        palabraActual = new StringBuilder();

        ponerEstadoJuego();

        tiempoRestante = tiempoMaximo;
        tiempoText.setText(String.valueOf(tiempoRestante));

        segmentoTiempo = tiempoRestante / 12;
        contadorTiempo = 0;
        indiceReloj = 0;
        indicePersonaje = 0;

        // Configurar el temporizador
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            tiempoText.setText(String.valueOf(--tiempoRestante));

            if (++contadorTiempo == segmentoTiempo) {
                contadorTiempo = 0;
                avanzarImagenReloj(++indiceReloj);
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

    private void detenerTemporizador() {
        if (timeline != null && timeline.getStatus() == Animation.Status.RUNNING) {
            timeline.stop();
        }
    }

    private void mostrarMensaje (String titulo, String mensaje, Image img) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);

        // Establecer el cuadro de diálogo como modal para bloquear la interacción con la ventana principal
        alert.initModality(Modality.APPLICATION_MODAL);

        // Añadir botón "OK"
        alert.getButtonTypes().setAll(ButtonType.OK);

        ImageView imagen = new ImageView (img);
        imagen.setFitWidth(80);
        imagen.setFitHeight(80);
        alert.setGraphic(imagen);

        // Mostrar el cuadro de diálogo y esperar a que el usuario lo cierre
        alert.show();
    }

    private void timeout() {
        perderJugadaTimeout();
    }

    private void mostrarMensajeTimeout() {
        String titulo = bundle.getString("alert.tiempo.agotado.title");
        String mensaje = bundle.getString("alert.tiempo.agotado.message");
        Image img = new Image(getClass().getResourceAsStream(bundle.getString("alert.reloj.lleno")));

        mostrarMensaje (titulo, mensaje, img);
    }

    private void mostrarMensajePerder() {
        String titulo = bundle.getString("alert.ahorcado.title");
        String mensaje = bundle.getString("alert.ahorcado.message");
        Image img = new Image(getClass().getResourceAsStream(bundle.getString("alert.ahorcado.imagen")));

        mostrarMensaje(titulo, mensaje, img);
    }

    private void perderJugadaTimeout() {
        if (dialogArriesgar != null ) {
            dialogArriesgar.close();
            dialogArriesgar = null;
        }

        detenerTemporizador();
        ponerEstadoDetenido();
        sonidoPerder();
        mostrarMensajeTimeout();
    }

    private void perderJugada() {
        detenerTemporizador();
        ponerEstadoDetenido();
        sonidoPerder();
        mostrarMensajePerder();
    }

    private void mostrarMensajeGanar() {
        //String titulo = bundle.getString("alert.ahorcado.title");
        //String mensaje = bundle.getString("alert.ahorcado.message");
        Image img = new Image(getClass().getResourceAsStream(bundle.getString("alert.ahorcado.imagen")));

        mostrarMensaje("Ganaste", "Adivinaste la palabra!", img);
    }

    private void ganarJugada() {
        detenerTemporizador();
        ponerEstadoDetenido();
        completarPalabraSecreta();
        mostrarPalabraAdivinar();
        //sonidoPerder();
        mostrarMensajeGanar();
    }


    private void completarPalabraSecreta() {
        for (int i = 0; i < palabraSecreta.length(); i++) {
            char letra = palabraSecreta.charAt(i);
            palabraActual.setCharAt(i, letra);
        }
    }


    private String obtenerPalabraSecreta() {
        PalabraDAO palabraDAO = new PalabraDAO();
        return palabraDAO.seleccionarPalabraPorCategoria("Paises");
    }

    private void sonidoError() {
        String rutaSonido = getClass().getResource("/sonidos/" + "error.wav").toExternalForm();
        reproducirSonido(rutaSonido);
    }

    private void sonidoPerder() {
        String rutaSonido = getClass().getResource("/sonidos/" + "lose.wav").toExternalForm();
        reproducirSonido(rutaSonido);
    }

    private void reproducirSonido(String rutaSonido) {
        try {
            Media media = new Media(rutaSonido);
            MediaPlayer mediaPlayer = new MediaPlayer(media);

            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.dispose(); // Liberar recursos después de reproducir el sonido
            });

            mediaPlayer.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
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
            if (palabraActual.toString().equals(palabraSecreta)) {
                ganarJugada();
            }
        }
        else {
            avanzarImagenPersonaje(++indicePersonaje);
            if (indicePersonaje == MAX_INTENTOS) {
                perderJugada();
            }
            else {
                Timeline timeline = getTimelineFondoError();
                timeline.play();

                sonidoError();
            }
        }
    }


    private Timeline getTimelineFondoError() {
        Paint fillOriginal = fondoDibujo.getFill();

        Duration medioSegundo = Duration.millis(700);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    fondoDibujo.setFill(Color.RED);
                    gridBotones.setDisable(true);
                }),
                new KeyFrame(medioSegundo, e -> {
                    fondoDibujo.setFill(fillOriginal);
                    gridBotones.setDisable(false);
                })
        );
        timeline.setCycleCount(1);
        return timeline;
    }

    private void mostrarPalabraAdivinar () {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < palabraActual.length(); i++) {
            builder.append(palabraActual.charAt(i)).append(' ');
        }

        palabraText.setText(builder.toString());
    }

    @FXML
    private void arriesgarPalabra() {
        String titulo = bundle.getString("input.arriesgar");
        String mensaje = bundle.getString("input.arriesgar.palabra");

        dialogArriesgar = new TextInputDialog();

        dialogArriesgar.setTitle(titulo);
        dialogArriesgar.setHeaderText(null);
        dialogArriesgar.initOwner(null);
        dialogArriesgar.setContentText(mensaje);

        Optional<TextField> resultado = Optional.ofNullable(dialogArriesgar.getEditor());

        resultado.ifPresent(textField -> {
            // Definir un operador que permita solo letras mayúsculas
            UnaryOperator<TextFormatter.Change> filter = change -> {
                String newText = change.getControlNewText();
                if (Pattern.matches("[a-zA-ZÑñ ]*", newText)) {
                    return change;
                } else {
                    return null;
                }
            };

            TextFormatter<String> textFormatter = new TextFormatter<>(filter);
            textField.setTextFormatter(textFormatter);
        });

        Optional<String> respuesta = dialogArriesgar.showAndWait();

        respuesta.ifPresent(palabraIngresada -> {
            if (palabraIngresada.toUpperCase().equals(palabraSecreta)) {
                dialogArriesgar.close();
                ganarJugada();
            }
        });
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
        ponerEstadoInicialGraficos();

        btnJugar.setDisable(true);
        btnArriesgar.setDisable(false);
        menuJugar.setDisable(true);
        btnDetener.setDisable(false);
        gridBotones.setDisable(false);
    }

    private void ponerEstadoDetenido() {
        btnJugar.setDisable(false);
        btnArriesgar.setDisable(true);
        menuJugar.setDisable(false);
        btnDetener.setDisable(true);
        gridBotones.setDisable(true);
    }

    private void ponerEstadoInicialGraficos() {
        reloj.setImage(imagenesReloj.get(0).getImagen());
        dibujo.setImage(imagenesPersonaje.get(0).getImagen());
    }

    private void avanzarImagenReloj(int indice) {
        reloj.setImage(imagenesReloj.get(indice).getImagen());
    }

    private void avanzarImagenPersonaje(int indice) {
        dibujo.setImage(imagenesPersonaje.get(indice).getImagen());
    }
}