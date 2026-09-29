package com.llorieb.ahorcado;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.util.Duration;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import java.util.function.UnaryOperator;


public class AhorcadoController {

    @FXML
    private ImageView dibujo;
    @FXML
    private Rectangle fondoDibujo;
    @FXML
    private ImageView reloj;
    @FXML
    private FlowPane palabraBox;
    @FXML
    private Label tiempoText;
    @FXML
    private Label categoriaValor;
    @FXML
    private StackPane rootPane;
    @FXML
    private Button btnMenuArchivo;
    @FXML
    private Button btnMenuAyuda;
    @FXML
    private VBox menuArchivoPopup;
    @FXML
    private VBox menuAyudaPopup;
    @FXML
    private Button menuJugar;
    @FXML
    private Button menuPreferencias;
    @FXML
    private Button btnJugar;
    @FXML
    private Button btnDetener;
    @FXML
    private Button btnArriesgar;
    @FXML
    private GridPane gridBotones;

    // Overlay moderno de fin de partida
    @FXML
    private StackPane resultadoOverlay;
    @FXML
    private VBox resultadoCard;
    @FXML
    private ImageView resultadoIcono;
    @FXML
    private Label resultadoTitulo;
    @FXML
    private Label resultadoMensaje;
    @FXML
    private Label resultadoPalabra;

    // Overlay moderno para arriesgar la respuesta
    @FXML
    private StackPane arriesgarOverlay;
    @FXML
    private VBox arriesgarCard;
    @FXML
    private TextField arriesgarCampo;
    @FXML
    private Button btnConfirmarArriesgar;

    // Overlay Cómo jugar
    @FXML
    private StackPane comoJugarOverlay;
    @FXML
    private VBox comoJugarCard;

    // Overlay Acerca de
    @FXML
    private StackPane acercaOverlay;
    @FXML
    private VBox acercaCard;

    private static final int TIEMPO_DEFAULT = 30;
    private static final int[] TIEMPOS_PERMITIDOS = {15, 30, 45, 60};
    private static final String PREF_TIEMPO_PARTIDA = "tiempoPartidaSegundos";

    private static final String CATEGORIA_DEFAULT = "Paises";
    private static final String[] CATEGORIAS_PERMITIDAS = {
            "Paises",
            "Ciudades",
            "Marcas de autos",
            "Bandas de Rock"
    };
    private static final String PREF_CATEGORIA = "categoriaJuego";

    private final Preferences preferencias = Preferences.userNodeForPackage(AhorcadoController.class);

    // tiempoMaximo es la preferencia elegida para la próxima partida.
    private int tiempoMaximo = TIEMPO_DEFAULT;
    private int tiempoRestante = TIEMPO_DEFAULT;
    private int duracionPartidaActual = TIEMPO_DEFAULT;
    private String categoriaSeleccionada = CATEGORIA_DEFAULT;
    private String categoriaPartidaActual = CATEGORIA_DEFAULT;
    private Timeline timeline;
    private int indiceReloj;
    private int indicePersonaje;
    private String palabraSecreta;
    private StringBuilder palabraActual;
    private ResourceBundle bundle;
    private final List<ImagenReloj> imagenesReloj = new ArrayList<>();
    private final List<ImagenPersonaje> imagenesPersonaje = new ArrayList<>();
    private final int MAX_INTENTOS = 6;

    private AudioClip sonidoErrorClip;
    private PauseTransition pausaError;
    private MediaPlayer reproductorFin;
    private MediaPlayer reproductorVictoria;
    private boolean audioPrecalentado;
    private Paint fondoOriginal;
    private boolean resultadoAnimado;
    private boolean reanudarTrasAcerca;
    private boolean reanudarTrasComoJugar;

    private static final double VOLUMEN_ERROR = 0.72;
    private static final double VOLUMEN_FIN = 0.88;
    private static final double VOLUMEN_VICTORIA = 0.86;

    private record PreferenciasJuego(int tiempo, String categoria) {
    }

    public void initialize() {
        bundle = ResourceBundle.getBundle("/properties/textos_es");
        configurarMenuIntegrado();

        tiempoMaximo = cargarTiempoPreferido();
        tiempoRestante = tiempoMaximo;
        duracionPartidaActual = tiempoMaximo;

        categoriaSeleccionada = cargarCategoriaPreferida();
        categoriaPartidaActual = categoriaSeleccionada;
        actualizarCategoriaVisible(categoriaSeleccionada);

        cargarImagenesReloj();
        cargarImagenesPersonaje();

        tiempoText.setText(String.valueOf(tiempoRestante));

        // Evita que el StackPane estire la tarjeta del resultado a toda la altura
        // disponible. La tarjeta conserva la altura natural de su contenido.
        resultadoCard.setMaxHeight(Region.USE_PREF_SIZE);

        // La tarjeta de Arriesgar también conserva únicamente la altura de su contenido.
        arriesgarCard.setMaxHeight(Region.USE_PREF_SIZE);
        configurarOverlayArriesgar();

        comoJugarCard.setMaxHeight(650.0);
        configurarOverlayComoJugar();

        acercaCard.setMaxHeight(Region.USE_PREF_SIZE);
        configurarOverlayAcercaDe();

        fondoOriginal = fondoDibujo.getFill();

        /*
         * Los efectos muy cortos se reproducen con AudioClip. A diferencia de
         * MediaPlayer, AudioClip está pensado para sonidos de baja latencia y
         * evita que la primera reproducción del soft-bup pierda su ataque.
         */
        sonidoErrorClip = crearAudioClip(
                "/sonidos/wrong_letter_soft_bup.wav",
                VOLUMEN_ERROR
        );

        pausaError = new PauseTransition(Duration.millis(250));
        pausaError.setOnFinished(event -> restaurarFondoDespuesDeError());

        reproductorFin = crearReproductor(
                "/sonidos/game_over_glass_descent.wav",
                VOLUMEN_FIN
        );

        reproductorVictoria = crearReproductor(
                "/sonidos/victory_glass_ascent.wav",
                VOLUMEN_VICTORIA
        );
    }


    /**
     * Menú superior dibujado dentro del propio Scene. A diferencia del
     * MenuBar nativo, no crea PopupWindow externos, así que comparte exactamente
     * la misma transformación responsive que el resto de la interfaz.
     */
    private void configurarMenuIntegrado() {
        if (rootPane == null) {
            return;
        }

        rootPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!hayMenuIntegradoAbierto()) {
                return;
            }

            Object target = event.getTarget();
            if (!(target instanceof Node node)) {
                cerrarMenusIntegrados();
                return;
            }

            if (!esDescendienteDe(node, btnMenuArchivo)
                    && !esDescendienteDe(node, btnMenuAyuda)
                    && !esDescendienteDe(node, menuArchivoPopup)
                    && !esDescendienteDe(node, menuAyudaPopup)) {
                cerrarMenusIntegrados();
            }
        });

        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && hayMenuIntegradoAbierto()) {
                cerrarMenusIntegrados();
                event.consume();
            }
        });
    }

    @FXML
    private void toggleMenuArchivo(ActionEvent event) {
        boolean abrir = menuArchivoPopup != null && !menuArchivoPopup.isVisible();
        cerrarMenusIntegrados();
        if (abrir) {
            mostrarMenuIntegrado(menuArchivoPopup, btnMenuArchivo);
        }
    }

    @FXML
    private void toggleMenuAyuda(ActionEvent event) {
        boolean abrir = menuAyudaPopup != null && !menuAyudaPopup.isVisible();
        cerrarMenusIntegrados();
        if (abrir) {
            mostrarMenuIntegrado(menuAyudaPopup, btnMenuAyuda);
        }
    }

    private void mostrarMenuIntegrado(VBox popup, Button ancla) {
        if (popup == null || ancla == null || rootPane == null) {
            return;
        }

        Bounds anclaEnScene = ancla.localToScene(ancla.getBoundsInLocal());
        if (anclaEnScene == null) {
            return;
        }
        Bounds anclaEnRoot = rootPane.sceneToLocal(anclaEnScene);

        /*
         * El popup es unmanaged para que StackPane no lo estire hasta ocupar
         * toda la altura disponible. Lo dimensionamos a su tamaño preferido y
         * lo ubicamos manualmente en las mismas coordenadas lógicas del root.
         * Como popup y ancla viven dentro del mismo árbol escalado, ambos
         * conservan la alineación en 100%, 125%, 150%, 175%, etc.
         */
        popup.applyCss();
        popup.autosize();
        popup.relocate(anclaEnRoot.getMinX(), anclaEnRoot.getMaxY());
        popup.setVisible(true);
        popup.toFront();

        ancla.getStyleClass().add("menu-superior-activo");
    }

    private void cerrarMenusIntegrados() {
        if (menuArchivoPopup != null) {
            menuArchivoPopup.setVisible(false);
        }
        if (menuAyudaPopup != null) {
            menuAyudaPopup.setVisible(false);
        }
        if (btnMenuArchivo != null) {
            btnMenuArchivo.getStyleClass().remove("menu-superior-activo");
        }
        if (btnMenuAyuda != null) {
            btnMenuAyuda.getStyleClass().remove("menu-superior-activo");
        }
    }

    private boolean hayMenuIntegradoAbierto() {
        return (menuArchivoPopup != null && menuArchivoPopup.isVisible())
                || (menuAyudaPopup != null && menuAyudaPopup.isVisible());
    }

    private boolean esDescendienteDe(Node node, Node ancestro) {
        if (node == null || ancestro == null) {
            return false;
        }
        Node actual = node;
        while (actual != null) {
            if (actual == ancestro) {
                return true;
            }
            actual = actual.getParent();
        }
        return false;
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
        cerrarMenusIntegrados();
        detenerEfectoError();
        ocultarResultado();
        ocultarArriesgarInmediato();
        ocultarComoJugarInmediato(false);
        ocultarAcercaDeInmediato(false);
        palabraActual = new StringBuilder();

        ponerEstadoJuego();
        reiniciarTeclado();

        // La categoría y la duración quedan fijadas al comenzar la partida.
        // Preferencias se deshabilita durante el juego para evitar que cambien
        // a mitad de una palabra.
        categoriaPartidaActual = categoriaSeleccionada;
        actualizarCategoriaVisible(categoriaPartidaActual);
        duracionPartidaActual = tiempoMaximo;
        tiempoRestante = duracionPartidaActual;
        tiempoText.setText(String.valueOf(tiempoRestante));

        indiceReloj = 0;
        indicePersonaje = 0;
        avanzarImagenReloj(0);

        // Configurar el temporizador. Las imágenes del reloj se seleccionan
        // según el porcentaje de tiempo transcurrido, por lo que 15, 30, 45
        // y 60 segundos recorren proporcionalmente la misma vuelta completa.
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            tiempoRestante = Math.max(0, tiempoRestante - 1);
            tiempoText.setText(String.valueOf(tiempoRestante));
            actualizarImagenRelojProporcional();

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

    /**
     * Abre las preferencias del juego. Tanto la categoría como el tiempo se
     * guardan con java.util.prefs y se conservan entre ejecuciones.
     */
    @FXML
    private void mostrarPreferencias(ActionEvent event) {
        cerrarMenusIntegrados();
        Dialog<PreferenciasJuego> dialog = new Dialog<>();
        dialog.setTitle("Preferencias");
        dialog.setHeaderText("Configuración de juego");
        dialog.initOwner(btnJugar.getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);

        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardar, cancelar);
        dialog.getDialogPane().setPrefWidth(410);
        dialog.getDialogPane().getStyleClass().add("preferencias-dialog");

        var css = getClass().getResource("/css/estilos.css");
        if (css != null) {
            dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        }

        Label descripcion = new Label(
                "Elegí la categoría de palabras y el tiempo disponible para cada partida."
        );
        descripcion.setWrapText(true);
        descripcion.getStyleClass().add("preferencias-descripcion");

        Label tituloCategoria = new Label("Categoría");
        tituloCategoria.getStyleClass().add("preferencias-seccion-titulo");

        ToggleGroup grupoCategorias = new ToggleGroup();
        VBox opcionesCategorias = new VBox(6.0);
        opcionesCategorias.getStyleClass().add("preferencias-opciones");

        for (String categoria : CATEGORIAS_PERMITIDAS) {
            RadioButton opcion = new RadioButton(nombreCategoriaVisible(categoria));
            opcion.setToggleGroup(grupoCategorias);
            opcion.setUserData(categoria);
            opcion.getStyleClass().add("preferencias-opcion");

            if (categoria.equals(categoriaSeleccionada)) {
                opcion.setSelected(true);
            }

            opcionesCategorias.getChildren().add(opcion);
        }

        Separator separador = new Separator();
        separador.getStyleClass().add("preferencias-separador");

        Label tituloTiempo = new Label("Tiempo por partida");
        tituloTiempo.getStyleClass().add("preferencias-seccion-titulo");

        ToggleGroup grupoTiempo = new ToggleGroup();
        VBox opcionesTiempo = new VBox(6.0);
        opcionesTiempo.getStyleClass().add("preferencias-opciones");

        for (int segundos : TIEMPOS_PERMITIDOS) {
            RadioButton opcion = new RadioButton(segundos + " segundos");
            opcion.setToggleGroup(grupoTiempo);
            opcion.setUserData(segundos);
            opcion.getStyleClass().add("preferencias-opcion");

            if (segundos == tiempoMaximo) {
                opcion.setSelected(true);
            }

            opcionesTiempo.getChildren().add(opcion);
        }

        VBox contenido = new VBox(12.0,
                descripcion,
                tituloCategoria,
                opcionesCategorias,
                separador,
                tituloTiempo,
                opcionesTiempo
        );
        contenido.getStyleClass().add("preferencias-panel");
        dialog.getDialogPane().setContent(contenido);

        Button botonGuardar = (Button) dialog.getDialogPane().lookupButton(guardar);
        botonGuardar.getStyleClass().add("preferencias-guardar");
        Button botonCancelar = (Button) dialog.getDialogPane().lookupButton(cancelar);
        botonCancelar.getStyleClass().add("preferencias-cancelar");

        dialog.setResultConverter(tipoBoton -> {
            if (tipoBoton != guardar
                    || grupoCategorias.getSelectedToggle() == null
                    || grupoTiempo.getSelectedToggle() == null) {
                return null;
            }

            String categoria = (String) grupoCategorias.getSelectedToggle().getUserData();
            int segundos = (Integer) grupoTiempo.getSelectedToggle().getUserData();
            return new PreferenciasJuego(segundos, categoria);
        });

        dialog.showAndWait().ifPresent(this::guardarPreferenciasJuego);
    }

    private int cargarTiempoPreferido() {
        int guardado = preferencias.getInt(PREF_TIEMPO_PARTIDA, TIEMPO_DEFAULT);

        for (int permitido : TIEMPOS_PERMITIDOS) {
            if (guardado == permitido) {
                return guardado;
            }
        }

        return TIEMPO_DEFAULT;
    }

    private String cargarCategoriaPreferida() {
        String guardada = preferencias.get(PREF_CATEGORIA, CATEGORIA_DEFAULT);

        for (String permitida : CATEGORIAS_PERMITIDAS) {
            if (permitida.equals(guardada)) {
                return guardada;
            }
        }

        return CATEGORIA_DEFAULT;
    }

    private void guardarPreferenciasJuego(PreferenciasJuego nuevasPreferencias) {
        tiempoMaximo = nuevasPreferencias.tiempo();
        categoriaSeleccionada = nuevasPreferencias.categoria();

        preferencias.putInt(PREF_TIEMPO_PARTIDA, tiempoMaximo);
        preferencias.put(PREF_CATEGORIA, categoriaSeleccionada);

        boolean partidaEnCurso = timeline != null
                && timeline.getStatus() == Animation.Status.RUNNING;

        // Fuera de una partida actualizamos inmediatamente la vista previa.
        if (!partidaEnCurso) {
            tiempoRestante = tiempoMaximo;
            duracionPartidaActual = tiempoMaximo;
            tiempoText.setText(String.valueOf(tiempoMaximo));
            indiceReloj = 0;
            avanzarImagenReloj(0);

            categoriaPartidaActual = categoriaSeleccionada;
            actualizarCategoriaVisible(categoriaSeleccionada);
        }
    }

    private void actualizarCategoriaVisible(String categoria) {
        if (categoriaValor != null) {
            categoriaValor.setText(nombreCategoriaVisible(categoria));
        }
    }

    private String nombreCategoriaVisible(String categoria) {
        return switch (categoria) {
            case "Paises" -> "Países";
            case "Ciudades" -> "Ciudades";
            case "Marcas de autos" -> "Marcas de autos";
            case "Bandas de Rock" -> "Bandas de Rock";
            default -> categoria;
        };
    }

    @FXML
    private void detenerJuego(ActionEvent event) {
        // Detener el Timeline y realizar otras acciones de interrupción si es necesario
        detenerTemporizador();
        detenerEfectoError();

        // Resto de la lógica para manejar la interrupción del juego
        ponerEstadoDetenido();
    }

    @FXML
    private void cerrarAplicacion(ActionEvent event) {
        cerrarMenusIntegrados();
        // Realizar cualquier limpieza o acciones necesarias antes de cerrar la aplicación
        detenerTemporizador();
        detenerEfectoError();
        detenerSonidosResultado();

        // Cerrar la aplicación
        Platform.exit();
    }

    @FXML
    private void reiniciarDesdeResultado(ActionEvent event) {
        ocultarResultado();
        iniciarJuego();
    }

    @FXML
    private void cerrarResultado(ActionEvent event) {
        ocultarResultado();
    }

    @FXML
    private void mostrarComoJugar(ActionEvent event) {
        cerrarMenusIntegrados();
        if (comoJugarOverlay == null || comoJugarCard == null) {
            return;
        }

        // La ayuda se comporta como un diálogo modal integrado. Si hay una
        // partida en curso, pausamos el reloj y lo retomamos al cerrar.
        reanudarTrasComoJugar = timeline != null
                && timeline.getStatus() == Animation.Status.RUNNING;
        if (reanudarTrasComoJugar) {
            timeline.pause();
        }

        comoJugarOverlay.setMouseTransparent(false);
        comoJugarOverlay.setVisible(true);
        comoJugarOverlay.toFront();
        comoJugarOverlay.setOpacity(0.0);

        comoJugarCard.setScaleX(0.95);
        comoJugarCard.setScaleY(0.95);

        FadeTransition fade = new FadeTransition(Duration.millis(170), comoJugarOverlay);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        ScaleTransition escala = new ScaleTransition(Duration.millis(170), comoJugarCard);
        escala.setFromX(0.95);
        escala.setFromY(0.95);
        escala.setToX(1.0);
        escala.setToY(1.0);
        escala.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, escala).play();
        Platform.runLater(comoJugarCard::requestFocus);
    }

    @FXML
    private void cerrarComoJugar(ActionEvent event) {
        ocultarComoJugarAnimado();
    }

    private void configurarOverlayComoJugar() {
        comoJugarOverlay.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && comoJugarOverlay.isVisible()) {
                ocultarComoJugarAnimado();
                event.consume();
            }
        });
    }

    private void ocultarComoJugarAnimado() {
        if (comoJugarOverlay == null || !comoJugarOverlay.isVisible()) {
            return;
        }

        FadeTransition fade = new FadeTransition(Duration.millis(120), comoJugarOverlay);
        fade.setFromValue(comoJugarOverlay.getOpacity());
        fade.setToValue(0.0);

        ScaleTransition escala = new ScaleTransition(Duration.millis(120), comoJugarCard);
        escala.setFromX(comoJugarCard.getScaleX());
        escala.setFromY(comoJugarCard.getScaleY());
        escala.setToX(0.97);
        escala.setToY(0.97);
        escala.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition salida = new ParallelTransition(fade, escala);
        salida.setOnFinished(event -> ocultarComoJugarInmediato(true));
        salida.play();
    }

    private void ocultarComoJugarInmediato(boolean reanudarJuego) {
        if (comoJugarOverlay == null) {
            return;
        }

        comoJugarOverlay.setVisible(false);
        comoJugarOverlay.setMouseTransparent(true);
        comoJugarOverlay.setOpacity(0.0);

        if (comoJugarCard != null) {
            comoJugarCard.setScaleX(1.0);
            comoJugarCard.setScaleY(1.0);
        }

        if (reanudarJuego && reanudarTrasComoJugar && timeline != null) {
            timeline.play();
        }
        reanudarTrasComoJugar = false;
    }

    @FXML
    private void mostrarAcercaDe(ActionEvent event) {
        cerrarMenusIntegrados();
        if (acercaOverlay == null || acercaCard == null) {
            return;
        }

        // Se comporta como un diálogo modal integrado: si había una partida
        // corriendo, pausamos el reloj y lo reanudamos al cerrar.
        reanudarTrasAcerca = timeline != null
                && timeline.getStatus() == Animation.Status.RUNNING;
        if (reanudarTrasAcerca) {
            timeline.pause();
        }

        acercaOverlay.setMouseTransparent(false);
        acercaOverlay.setVisible(true);
        acercaOverlay.toFront();
        acercaOverlay.setOpacity(0.0);

        acercaCard.setScaleX(0.95);
        acercaCard.setScaleY(0.95);

        FadeTransition fade = new FadeTransition(Duration.millis(170), acercaOverlay);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        ScaleTransition escala = new ScaleTransition(Duration.millis(170), acercaCard);
        escala.setFromX(0.95);
        escala.setFromY(0.95);
        escala.setToX(1.0);
        escala.setToY(1.0);
        escala.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, escala).play();
        Platform.runLater(acercaCard::requestFocus);
    }

    @FXML
    private void cerrarAcercaDe(ActionEvent event) {
        ocultarAcercaDeAnimado();
    }

    private void configurarOverlayAcercaDe() {
        acercaOverlay.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && acercaOverlay.isVisible()) {
                ocultarAcercaDeAnimado();
                event.consume();
            }
        });
    }

    private void ocultarAcercaDeAnimado() {
        if (acercaOverlay == null || !acercaOverlay.isVisible()) {
            return;
        }

        FadeTransition fade = new FadeTransition(Duration.millis(120), acercaOverlay);
        fade.setFromValue(acercaOverlay.getOpacity());
        fade.setToValue(0.0);

        ScaleTransition escala = new ScaleTransition(Duration.millis(120), acercaCard);
        escala.setFromX(acercaCard.getScaleX());
        escala.setFromY(acercaCard.getScaleY());
        escala.setToX(0.97);
        escala.setToY(0.97);
        escala.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition salida = new ParallelTransition(fade, escala);
        salida.setOnFinished(event -> ocultarAcercaDeInmediato(true));
        salida.play();
    }

    private void ocultarAcercaDeInmediato(boolean reanudarJuego) {
        if (acercaOverlay == null) {
            return;
        }

        acercaOverlay.setVisible(false);
        acercaOverlay.setMouseTransparent(true);
        acercaOverlay.setOpacity(0.0);

        if (acercaCard != null) {
            acercaCard.setScaleX(1.0);
            acercaCard.setScaleY(1.0);
        }

        if (reanudarJuego && reanudarTrasAcerca && timeline != null) {
            timeline.play();
        }
        reanudarTrasAcerca = false;
    }

    /**
     * Configura una sola vez el campo de Arriesgar. El filtro sólo limita la
     * longitud: al existir categorías como bandas y marcas, conviene aceptar
     * números y signos (U2, AC/DC, R.E.M., Guns N' Roses, etc.).
     */
    private void configurarOverlayArriesgar() {
        UnaryOperator<TextFormatter.Change> filtro = change ->
                change.getControlNewText().length() <= 60 ? change : null;

        arriesgarCampo.setTextFormatter(new TextFormatter<>(filtro));

        arriesgarCampo.textProperty().addListener((obs, anterior, actual) ->
                btnConfirmarArriesgar.setDisable(
                        actual == null || actual.trim().isEmpty()
                )
        );

        // El evento del TextField burbujea hasta el overlay. Escape equivale
        // a Cancelar y no altera el tiempo ni el estado de la partida.
        arriesgarOverlay.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && arriesgarOverlay.isVisible()) {
                ocultarArriesgarAnimado();
                event.consume();
            }
        });
    }

    private void mostrarArriesgarOverlay() {
        arriesgarCampo.clear();
        btnConfirmarArriesgar.setDisable(true);

        arriesgarOverlay.setMouseTransparent(false);
        arriesgarOverlay.setVisible(true);
        arriesgarOverlay.toFront();
        arriesgarOverlay.setOpacity(0.0);

        arriesgarCard.setScaleX(0.95);
        arriesgarCard.setScaleY(0.95);

        FadeTransition fade = new FadeTransition(Duration.millis(170), arriesgarOverlay);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        ScaleTransition escala = new ScaleTransition(Duration.millis(170), arriesgarCard);
        escala.setFromX(0.95);
        escala.setFromY(0.95);
        escala.setToX(1.0);
        escala.setToY(1.0);
        escala.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, escala).play();

        // El requestFocus se hace en el siguiente pulso de JavaFX, cuando el
        // nodo ya está visible en la Scene.
        Platform.runLater(() -> {
            arriesgarCampo.requestFocus();
            arriesgarCampo.positionCaret(arriesgarCampo.getText().length());
        });
    }

    @FXML
    private void cancelarArriesgar(ActionEvent event) {
        ocultarArriesgarAnimado();
    }

    private void ocultarArriesgarAnimado() {
        if (!arriesgarOverlay.isVisible()) {
            return;
        }

        FadeTransition fade = new FadeTransition(Duration.millis(120), arriesgarOverlay);
        fade.setFromValue(arriesgarOverlay.getOpacity());
        fade.setToValue(0.0);

        ScaleTransition escala = new ScaleTransition(Duration.millis(120), arriesgarCard);
        escala.setFromX(arriesgarCard.getScaleX());
        escala.setFromY(arriesgarCard.getScaleY());
        escala.setToX(0.97);
        escala.setToY(0.97);
        escala.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition salida = new ParallelTransition(fade, escala);
        salida.setOnFinished(event -> ocultarArriesgarInmediato());
        salida.play();
    }

    private void ocultarArriesgarInmediato() {
        if (arriesgarOverlay == null) {
            return;
        }

        arriesgarOverlay.setVisible(false);
        arriesgarOverlay.setMouseTransparent(true);
        arriesgarOverlay.setOpacity(0.0);

        if (arriesgarCard != null) {
            arriesgarCard.setScaleX(1.0);
            arriesgarCard.setScaleY(1.0);
        }

        if (arriesgarCampo != null) {
            arriesgarCampo.clear();
        }
    }

    private void detenerTemporizador() {
        if (timeline != null && timeline.getStatus() == Animation.Status.RUNNING) {
            timeline.stop();
        }
    }

    private void timeout() {
        perderJugadaTimeout();
    }

    /**
     * Muestra una tarjeta moderna dentro de la propia ventana, evitando el
     * aspecto nativo de los Alert de JavaFX. La entrada combina un fade con
     * una escala muy corta y no bloquea el hilo de JavaFX.
     */
    private void mostrarResultadoFinPartida(String titulo, String mensaje, Image imagen, String claseEstado) {
        resultadoTitulo.setText(titulo);
        resultadoMensaje.setText(mensaje);
        resultadoPalabra.setText(palabraSecreta == null ? "" : palabraSecreta);

        /*
         * La imagen 106.png del ahorcado contiene más margen interno que las
         * imágenes del reloj. Por eso usamos una caja algo mayor en derrota:
         * el contenido visible termina teniendo prácticamente la misma
         * presencia que el reloj del overlay de tiempo agotado.
         */
        boolean usaPersonaje = "resultado-derrota".equals(claseEstado)
                || "resultado-victoria".equals(claseEstado);
        double tamanoIcono = usaPersonaje ? 125.0 : 92.0;
        resultadoIcono.setFitWidth(tamanoIcono);
        resultadoIcono.setFitHeight(tamanoIcono);
        resultadoIcono.setPreserveRatio(true);
        resultadoIcono.setSmooth(true);
        resultadoIcono.setImage(imagen);

        resultadoCard.getStyleClass().removeAll(
                "resultado-tiempo",
                "resultado-derrota",
                "resultado-victoria"
        );
        resultadoCard.getStyleClass().add(claseEstado);

        resultadoOverlay.setMouseTransparent(false);
        resultadoOverlay.setVisible(true);
        resultadoOverlay.toFront();
        resultadoOverlay.setOpacity(0.0);

        resultadoCard.setScaleX(0.94);
        resultadoCard.setScaleY(0.94);
        resultadoAnimado = false;

        /*
         * Cada resultado se anima cuando su sonido realmente entra en PLAYING.
         * De esta forma la tarjeta y el feedback sonoro comienzan juntos sin
         * bloquear el hilo de JavaFX. Si el audio falla, el overlay se muestra
         * igualmente como fallback.
         */
        if ("resultado-victoria".equals(claseEstado)) {
            reproducirSonidoResultadoYAnimar(
                    reproductorVictoria,
                    "victoria"
            );
        } else {
            reproducirSonidoResultadoYAnimar(
                    reproductorFin,
                    "fin de partida"
            );
        }
    }

    private void mostrarResultadoTimeout() {
        Image img = new Image(getClass().getResourceAsStream("/images/sw12.png"));
        mostrarResultadoFinPartida(
                "¡Se acabó el tiempo!",
                "La palabra era",
                img,
                "resultado-tiempo"
        );
    }

    private void mostrarResultadoPerder() {
        Image img = new Image(getClass().getResourceAsStream("/images/106.png"));
        mostrarResultadoFinPartida(
                "¡Te quedaste sin intentos!",
                "La palabra era",
                img,
                "resultado-derrota"
        );
    }

    private void mostrarResultadoArriesgoIncorrecto() {
        Image img = new Image(getClass().getResourceAsStream("/images/106.png"));
        mostrarResultadoFinPartida(
                "¡La respuesta no era correcta!",
                "La palabra era",
                img,
                "resultado-derrota"
        );
    }

    private void mostrarResultadoGanar() {
        Image img = new Image(getClass().getResourceAsStream("/images/100.png"));
        mostrarResultadoFinPartida(
                "¡Adivinaste la palabra!",
                "La palabra era",
                img,
                "resultado-victoria"
        );
    }

    private void animarResultadoFinPartida() {
        if (resultadoAnimado || !resultadoOverlay.isVisible()) {
            return;
        }

        resultadoAnimado = true;

        FadeTransition fade = new FadeTransition(Duration.millis(180), resultadoOverlay);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        ScaleTransition escala = new ScaleTransition(Duration.millis(180), resultadoCard);
        escala.setFromX(0.94);
        escala.setFromY(0.94);
        escala.setToX(1.0);
        escala.setToY(1.0);
        escala.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, escala).play();
    }

    private void reproducirSonidoResultadoYAnimar(MediaPlayer reproductor, String descripcion) {
        if (reproductor == null) {
            animarResultadoFinPartida();
            return;
        }

        detenerSonidosResultado();

        reproductor.seek(Duration.ZERO);
        reproductor.setOnPlaying(this::animarResultadoFinPartida);
        reproductor.setOnError(() -> {
            System.err.println(
                    "Error reproduciendo sonido de " + descripcion + ": "
                            + reproductor.getError()
            );
            animarResultadoFinPartida();
        });

        reproductor.play();
    }

    private void detenerSonidosResultado() {
        if (reproductorFin != null) {
            reproductorFin.stop();
            reproductorFin.seek(Duration.ZERO);
        }

        if (reproductorVictoria != null) {
            reproductorVictoria.stop();
            reproductorVictoria.seek(Duration.ZERO);
        }
    }

    /**
     * Fuerza la inicialización real del mezclador/dispositivo de audio al arrancar.
     *
     * AudioClip ya mantiene el WAV decodificado en memoria, pero en algunos equipos
     * Windows la primera salida de audio de JavaFX puede perder parte del ataque
     * mientras se abre el dispositivo. Reproducir el mismo clip una vez a un volumen
     * prácticamente inaudible hace que ese costo ocurra antes de la primera jugada.
     */
    public void precalentarAudio() {
        if (audioPrecalentado || sonidoErrorClip == null) {
            return;
        }

        audioPrecalentado = true;

        // 0.0001 equivale aproximadamente a -80 dB respecto del nivel completo:
        // fuerza la reproducción real sin que resulte audible para el usuario.
        sonidoErrorClip.play(0.0001);
    }

    private AudioClip crearAudioClip(String recurso, double volumen) {
        try {
            var url = getClass().getResource(recurso);

            if (url == null) {
                System.err.println("No se encontró el recurso de audio: " + recurso);
                return null;
            }

            AudioClip clip = new AudioClip(url.toExternalForm());
            clip.setVolume(volumen);
            return clip;

        } catch (Exception e) {
            System.err.println("No se pudo inicializar el audio " + recurso);
            e.printStackTrace();
            return null;
        }
    }

    private MediaPlayer crearReproductor(String recurso, double volumen) {
        try {
            var url = getClass().getResource(recurso);

            if (url == null) {
                System.err.println("No se encontró el recurso de audio: " + recurso);
                return null;
            }

            MediaPlayer player = new MediaPlayer(new Media(url.toExternalForm()));
            player.setVolume(volumen);
            return player;

        } catch (Exception e) {
            System.err.println("No se pudo inicializar el audio " + recurso);
            e.printStackTrace();
            return null;
        }
    }

    private void detenerEfectoError() {
        if (pausaError != null) {
            pausaError.stop();
        }

        if (sonidoErrorClip != null) {
            sonidoErrorClip.stop();
        }

        if (fondoOriginal != null) {
            fondoDibujo.setFill(fondoOriginal);
        }
    }

    private void ocultarResultado() {
        if (resultadoOverlay == null) {
            return;
        }

        detenerSonidosResultado();

        resultadoAnimado = false;
        resultadoOverlay.setVisible(false);
        resultadoOverlay.setMouseTransparent(true);
        resultadoOverlay.setOpacity(0.0);

        if (resultadoCard != null) {
            resultadoCard.setScaleX(1.0);
            resultadoCard.setScaleY(1.0);
        }
    }

    private void perderJugadaTimeout() {
        ocultarArriesgarInmediato();
        detenerTemporizador();
        detenerEfectoError();
        ponerEstadoDetenido();
        completarPalabraSecreta();
        mostrarPalabraAdivinar();
        mostrarResultadoTimeout();
    }

    private void perderJugada() {
        ocultarArriesgarInmediato();
        detenerTemporizador();
        detenerEfectoError();
        ponerEstadoDetenido();
        completarPalabraSecreta();
        mostrarPalabraAdivinar();
        mostrarResultadoPerder();
    }

    private void perderJugadaPorArriesgo() {
        ocultarArriesgarInmediato();
        detenerTemporizador();
        detenerEfectoError();
        ponerEstadoDetenido();
        avanzarImagenPersonaje(MAX_INTENTOS);
        completarPalabraSecreta();
        mostrarPalabraAdivinar();
        mostrarResultadoArriesgoIncorrecto();
    }

    private void ganarJugada() {
        ocultarArriesgarInmediato();
        detenerTemporizador();
        detenerEfectoError();
        ponerEstadoDetenido();
        completarPalabraSecreta();
        mostrarPalabraAdivinar();
        mostrarResultadoGanar();
    }


    private void completarPalabraSecreta() {
        for (int i = 0; i < palabraSecreta.length(); i++) {
            char letra = palabraSecreta.charAt(i);
            palabraActual.setCharAt(i, letra);
        }
    }


    private String obtenerPalabraSecreta() {
        PalabraDAO palabraDAO = new PalabraDAO();
        String palabra = palabraDAO.seleccionarPalabraPorCategoria(categoriaPartidaActual);

        if (palabra == null) {
            return "";
        }

        return palabra.trim().toUpperCase(Locale.ROOT);
    }

    private void sonidoError() {
        /*
         * El feedback visual empieza inmediatamente. AudioClip mantiene el
         * WAV corto listo para reproducción y no bloquea el hilo de JavaFX.
         */
        fondoDibujo.setFill(Color.RED);
        gridBotones.setDisable(true);

        if (pausaError != null) {
            pausaError.stop();
            pausaError.playFromStart();
        }

        if (sonidoErrorClip != null) {
            sonidoErrorClip.stop();
            sonidoErrorClip.play();
        }
    }

    @FXML
    private void checkLetter(ActionEvent event) {
        Button button = (Button) event.getSource();
        char letraElegida = button.getText().charAt(0);
        char letraNormalizada = normalizarLetra(letraElegida);

        boolean letraCorrecta = false;

        // Una letra elegida no puede volver a pulsarse durante esta partida.
        button.setDisable(true);
        button.getStyleClass().removeAll("letra-correcta", "letra-incorrecta");

        /*
         * La comparación ignora tildes y diéresis, pero conserva la Ñ como
         * una letra distinta. Por ejemplo, pulsar A descubre tanto A como Á.
         * La letra que se muestra siempre es la original de palabraSecreta.
         */
        for (int i = 0; i < palabraSecreta.length(); i++) {
            char caracterSecreto = palabraSecreta.charAt(i);

            if (Character.isLetter(caracterSecreto)
                    && normalizarLetra(caracterSecreto) == letraNormalizada) {
                palabraActual.setCharAt(i, caracterSecreto);
                letraCorrecta = true;
            }
        }

        if (letraCorrecta) {
            button.getStyleClass().add("letra-correcta");
            mostrarPalabraAdivinar();

            if (palabraResuelta()) {
                ganarJugada();
            }
        } else {
            button.getStyleClass().add("letra-incorrecta");
            avanzarImagenPersonaje(++indicePersonaje);

            if (indicePersonaje == MAX_INTENTOS) {
                perderJugada();
            } else {
                sonidoError();
            }
        }
    }

    /**
     * Devuelve true cuando ya no quedan letras ocultas.
     */
    private boolean palabraResuelta() {
        return palabraActual.indexOf("_") < 0;
    }

    /**
     * Normaliza una letra para la lógica de juego.
     *
     * A, Á, À, Ä, Â -> A
     * E, É, Ë...    -> E
     * Ñ se conserva como Ñ y no se confunde con N.
     */
    private char normalizarLetra(char letra) {
        char mayuscula = Character.toUpperCase(letra);

        if (mayuscula == 'Ñ') {
            return 'Ñ';
        }

        String descompuesta = Normalizer.normalize(
                String.valueOf(mayuscula),
                Normalizer.Form.NFD
        );

        for (int i = 0; i < descompuesta.length(); i++) {
            char c = descompuesta.charAt(i);

            if (Character.getType(c) != Character.NON_SPACING_MARK) {
                return Character.toUpperCase(c);
            }
        }

        return mayuscula;
    }

    /**
     * Normaliza una palabra o frase para comparar respuestas completas.
     *
     * - ignora mayúsculas/minúsculas y tildes;
     * - conserva la Ñ como una letra distinta;
     * - conserva los números;
     * - ignora espacios y signos de puntuación.
     *
     * Así, por ejemplo, "ACDC" también puede acertar "AC/DC" y
     * "MERCEDES BENZ" puede acertar "MERCEDES-BENZ".
     */
    private String normalizarParaComparacion(String texto) {
        if (texto == null) {
            return "";
        }

        String limpio = texto.trim().toUpperCase(Locale.ROOT);
        StringBuilder normalizado = new StringBuilder(limpio.length());

        for (int i = 0; i < limpio.length(); i++) {
            char c = limpio.charAt(i);

            if (Character.isLetter(c)) {
                normalizado.append(normalizarLetra(c));
            } else if (Character.isDigit(c)) {
                normalizado.append(c);
            }
        }

        return normalizado.toString();
    }



    /*
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
    */

    private void mostrarPalabraAdivinar() {
        palabraBox.getChildren().clear();

        if (palabraSecreta == null || palabraSecreta.isBlank()) {
            Label placeholder = new Label("Sin palabra");
            placeholder.getStyleClass().add("palabra-placeholder");
            palabraBox.getChildren().add(placeholder);
            return;
        }

        /*
         * Algunas respuestas largas y muy conocidas (por ejemplo
         * EMIRATOS ÁRABES UNIDOS o RED HOT CHILI PEPPERS) necesitarían tres
         * líneas con el tamaño normal. En esos casos se compacta de forma
         * moderada toda la frase para conservar un máximo de dos líneas.
         */
        boolean compactaGlobal = requiereModoCompactoGlobal(palabraSecreta);
        palabraBox.setHgap(compactaGlobal ? 7.0 : 16.0);

        /*
         * Cada palabra se crea como un HBox independiente. El FlowPane puede
         * mover el bloque completo a la segunda línea, pero nunca corta una
         * palabra por la mitad.
         */
        int inicioPalabra = 0;

        for (int i = 0; i <= palabraSecreta.length(); i++) {
            boolean finDeFrase = i == palabraSecreta.length();
            boolean esEspacio = !finDeFrase && palabraSecreta.charAt(i) == ' ';

            if (finDeFrase || esEspacio) {
                if (i > inicioPalabra) {
                    palabraBox.getChildren().add(
                            crearGrupoPalabra(
                                    inicioPalabra,
                                    i,
                                    compactaGlobal
                            )
                    );
                }

                inicioPalabra = i + 1;
            }
        }
    }

    /**
     * Estima cuántas líneas ocuparía una frase con el tamaño normal actual.
     * Si supera dos líneas se activa el modo compacto global.
     */
    private boolean requiereModoCompactoGlobal(String frase) {
        final double anchoDisponible = 430.0;
        final double espacioEntrePalabras = 16.0;

        double anchoLinea = 0.0;
        int lineas = 1;

        for (String palabra : frase.split(" ")) {
            if (palabra.isBlank()) {
                continue;
            }

            double anchoGrupo = estimarAnchoGrupo(palabra, false);

            if (anchoGrupo > anchoDisponible) {
                return true;
            }

            if (anchoLinea == 0.0) {
                anchoLinea = anchoGrupo;
            } else if (anchoLinea + espacioEntrePalabras + anchoGrupo
                    <= anchoDisponible) {
                anchoLinea += espacioEntrePalabras + anchoGrupo;
            } else {
                lineas++;
                anchoLinea = anchoGrupo;

                if (lineas > 2) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Calcula aproximadamente el ancho visual de un bloque de palabra usando
     * las mismas medidas definidas en CSS.
     */
    private double estimarAnchoGrupo(
            String palabra,
            boolean compactaGlobal
    ) {
        int cantidadCaracteres = palabra.length();
        boolean muyCompacta = cantidadCaracteres > 13;
        boolean compacta = compactaGlobal || cantidadCaracteres > 11;

        double anchoLetra;
        double espacioInterno;

        if (muyCompacta) {
            anchoLetra = 27.0;
            espacioInterno = 2.0;
        } else if (compacta) {
            anchoLetra = 30.0;
            espacioInterno = 3.0;
        } else {
            anchoLetra = 34.0;
            espacioInterno = 5.0;
        }

        double ancho = 0.0;

        for (int i = 0; i < palabra.length(); i++) {
            char c = palabra.charAt(i);
            ancho += Character.isLetter(c) ? anchoLetra : 10.0;

            if (i < palabra.length() - 1) {
                ancho += espacioInterno;
            }
        }

        return ancho;
    }

    /**
     * Crea una palabra como bloque visual indivisible.
     *
     * Las palabras normales conservan las celdas de 34 px. Las palabras
     * largas, o una frase que de otro modo requeriría tres líneas, usan una
     * variante compacta. Sólo tokens de más de 13 caracteres usan la variante
     * "muy compacta" de 27 px.
     */
    private HBox crearGrupoPalabra(
            int inicio,
            int fin,
            boolean compactaGlobal
    ) {
        int cantidadCaracteres = fin - inicio;
        boolean muyCompacta = cantidadCaracteres > 13;
        boolean compacta = compactaGlobal || cantidadCaracteres > 11;

        double separacion = muyCompacta
                ? 2.0
                : (compacta ? 3.0 : 5.0);

        HBox grupo = new HBox(separacion);
        grupo.setAlignment(javafx.geometry.Pos.CENTER);
        grupo.getStyleClass().add("grupo-palabra");

        if (compacta) {
            grupo.getStyleClass().add("grupo-palabra-compacto");
        }

        if (muyCompacta) {
            grupo.getStyleClass().add("grupo-palabra-muy-compacto");
        }

        for (int i = inicio; i < fin; i++) {
            char secreto = palabraSecreta.charAt(i);
            char visible = palabraActual.charAt(i);

            if (Character.isLetter(secreto)) {
                Label celda = new Label();
                celda.getStyleClass().add("celda-palabra");

                if (visible == '_') {
                    celda.setText("");
                } else {
                    // Se muestra el carácter original, incluidos sus acentos.
                    celda.setText(String.valueOf(secreto));
                    celda.getStyleClass().add("celda-revelada");
                }

                grupo.getChildren().add(celda);
            } else {
                // Guiones, apóstrofes y otros signos están visibles desde el inicio.
                Label separador = new Label(String.valueOf(secreto));
                separador.getStyleClass().add("separador-palabra");
                grupo.getChildren().add(separador);
            }
        }

        return grupo;
    }



    @FXML
    private void arriesgarPalabra() {
        mostrarArriesgarOverlay();
    }

    /**
     * Enter en el campo y el botón principal llegan a este mismo método.
     * La comparación sigue ignorando mayúsculas y tildes, pero conserva la Ñ.
     */
    @FXML
    private void confirmarArriesgar(ActionEvent event) {
        String respuesta = arriesgarCampo.getText();

        if (respuesta == null || respuesta.trim().isEmpty()) {
            return;
        }

        if (normalizarParaComparacion(respuesta)
                .equals(normalizarParaComparacion(palabraSecreta))) {
            ganarJugada();
        } else {
            perderJugadaPorArriesgo();
        }
    }


    private void enmascararPalabraAdivinar() {
        for (char c : palabraSecreta.toCharArray()) {
            if (Character.isLetter(c)) {
                palabraActual.append("_");
            } else {
                // Espacios, guiones y apóstrofes se muestran desde el comienzo.
                palabraActual.append(c);
            }
        }
    }

    /**
     * Restaura todos los botones del teclado al comenzar una nueva partida.
     * Se eliminan los colores de acierto/error y se vuelven a habilitar.
     */
    private void reiniciarTeclado() {
        gridBotones.getChildren().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .forEach(button -> {
                    button.setDisable(false);
                    button.getStyleClass().removeAll("letra-correcta", "letra-incorrecta");
                });
    }

    private void ponerEstadoJuego() {
        ponerEstadoInicialGraficos();

        btnJugar.setDisable(true);
        btnArriesgar.setDisable(false);
        menuJugar.setDisable(true);
        menuPreferencias.setDisable(true);
        btnDetener.setDisable(false);
        gridBotones.setDisable(false);
    }

    private void ponerEstadoDetenido() {
        btnJugar.setDisable(false);
        btnArriesgar.setDisable(true);
        menuJugar.setDisable(false);
        menuPreferencias.setDisable(false);
        btnDetener.setDisable(true);
        gridBotones.setDisable(true);
    }

    private void ponerEstadoInicialGraficos() {
        reloj.setImage(imagenesReloj.get(0).getImagen());
        dibujo.setImage(imagenesPersonaje.get(0).getImagen());
    }

    /**
     * Mapea el porcentaje de tiempo transcurrido al índice de las imágenes
     * sw0..sw12. Se usa floor() para repartir proporcionalmente los pasos y
     * reservar sw12 (reloj agotado) exactamente para el segundo 0.
     */
    private void actualizarImagenRelojProporcional() {
        if (duracionPartidaActual <= 0 || imagenesReloj.isEmpty()) {
            return;
        }

        int ultimoIndice = imagenesReloj.size() - 1;
        int tiempoTranscurrido = duracionPartidaActual - tiempoRestante;

        int nuevoIndice = (int) Math.floor(
                (double) tiempoTranscurrido * ultimoIndice / duracionPartidaActual
        );

        nuevoIndice = Math.max(0, Math.min(ultimoIndice, nuevoIndice));

        if (nuevoIndice != indiceReloj) {
            indiceReloj = nuevoIndice;
            avanzarImagenReloj(indiceReloj);
        }
    }

    private void avanzarImagenReloj(int indice) {
        if (indice >= 0 && indice < imagenesReloj.size()) {
            reloj.setImage(imagenesReloj.get(indice).getImagen());
        }
    }

    private void avanzarImagenPersonaje(int indice) {
        dibujo.setImage(imagenesPersonaje.get(indice).getImagen());
    }

    private void restaurarFondoDespuesDeError() {
        fondoDibujo.setFill(fondoOriginal);

        // Sólo se reactiva el teclado si la partida sigue en curso.
        if (!btnDetener.isDisabled()) {
            gridBotones.setDisable(false);
        }
    }
}