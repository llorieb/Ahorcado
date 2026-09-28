package com.llorieb.ahorcado;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

public class Main extends Application {

    private static final double DESIGN_WIDTH = 480.0;
    private static final double DESIGN_HEIGHT = 785.0;

    // Estimación inicial de la decoración de ventana. Después del show se
    // reemplaza por la medida real del sistema operativo.
    private static final double INITIAL_CHROME_WIDTH = 20.0;
    private static final double INITIAL_CHROME_HEIGHT = 45.0;

    // Pequeño margen para que la ventana no quede pegada al borde inferior.
    private static final double SAFETY_MARGIN = 6.0;

    private Stage stage;
    private Scene scene;
    private ResponsivePane responsiveRoot;
    private PauseTransition reajustePendiente;
    private Timeline monitorPantalla;
    private boolean primeraApertura = true;
    private boolean ajustandoVentana = false;
    private boolean forzandoActualizacionDpi = false;
    private long ultimoForzadoDpiNanos = 0L;

    private static final double DPI_TOLERANCE = 0.01;
    private static final long DPI_NUDGE_COOLDOWN_NANOS = 350_000_000L;

    // Evita relayouts/sizeToScene innecesarios cuando la escala calculada ya
    // es la que está aplicada. Esto es especialmente importante en 125/150/175%,
    // donde Screen y Stage pueden conservar pequeñas diferencias de DPI sin que
    // exista un cambio visual real.
    private static final double SCALE_APPLY_TOLERANCE = 0.0005;
    private static final double SCENE_SIZE_TOLERANCE = 1.0;

    /*
     * Tras un cambio de escalado de Windows, Stage/Screen no siempre se
     * estabilizan en el mismo pulso. Mantenemos unas pocas pasadas de
     * recentrado para que la barra de título nunca pueda quedar fuera del
     * área visible aunque el DPI termine de aplicarse de forma asíncrona.
     */
    private static final int RECENTER_PASSES_AFTER_DPI_CHANGE = 6;
    private int pasadasRecentradoPendientes = 0;

    // Últimas métricas conocidas del monitor. En Windows, al cambiar el
    // escalado (100% -> 150%, etc.) JavaFX puede tardar en notificar el
    // cambio al Stage hasta que la ventana se mueve. El monitor periódico
    // permite reaccionar sin ninguna interacción del usuario.
    private double ultimaVisualX = Double.NaN;
    private double ultimaVisualY = Double.NaN;
    private double ultimaVisualWidth = Double.NaN;
    private double ultimaVisualHeight = Double.NaN;
    private double ultimaEscalaPantallaX = Double.NaN;
    private double ultimaEscalaPantallaY = Double.NaN;
    private double ultimaEscalaStageX = Double.NaN;
    private double ultimaEscalaStageY = Double.NaN;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/AhorcadoLayout.fxml"));
        Parent content = loader.load();

        this.stage = primaryStage;
        configurarStage(primaryStage);

        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double initialScale = calcularEscala(
                visualBounds,
                INITIAL_CHROME_WIDTH,
                INITIAL_CHROME_HEIGHT
        );

        /*
         * Siempre usamos el mismo contenedor, tanto a 100% como a 125/150%.
         * Esto evita tener dos caminos de layout distintos y permite cambiar
         * el escalado de Windows mientras Ahorcado está abierto.
         */
        responsiveRoot = new ResponsivePane(content, initialScale);
        scene = new Scene(responsiveRoot);

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);

        // Ocultamos sólo el primer ajuste para que no haya un salto visual.
        primaryStage.setOpacity(0.0);
        primaryStage.show();

        instalarReajusteDinamico(primaryStage);
        iniciarMonitorPantalla();

        Platform.runLater(() -> {
            ajustarVentanaAlAreaDisponible(true);
            primaryStage.setOpacity(1.0);

            AhorcadoController controller = loader.getController();
            controller.precalentarAudio();
        });
    }

    private void configurarStage(Stage stage) {
        stage.setTitle("Ahorcado");
        stage.getIcons().add(
                new Image(getClass().getResourceAsStream("/images/app-icon.png"))
        );
    }

    private void instalarReajusteDinamico(Stage stage) {
        reajustePendiente = new PauseTransition(Duration.millis(120));
        reajustePendiente.setOnFinished(event -> ajustarVentanaAlAreaDisponible(false));

        // Camino normal: JavaFX notifica el cambio de escala de salida.
        stage.outputScaleXProperty().addListener((obs, oldValue, newValue) -> {
            marcarRecentradoTrasCambioDpi();
            invalidarMetricasPantalla();
            programarReajuste();
        });
        stage.outputScaleYProperty().addListener((obs, oldValue, newValue) -> {
            marcarRecentradoTrasCambioDpi();
            invalidarMetricasPantalla();
            programarReajuste();
        });

        // Si la ventana pasa a otro monitor, recalculamos también.
        stage.xProperty().addListener((obs, oldValue, newValue) -> {
            if (!forzandoActualizacionDpi) {
                verificarCambiosPantalla();
            }
        });
        stage.yProperty().addListener((obs, oldValue, newValue) -> {
            if (!forzandoActualizacionDpi) {
                verificarCambiosPantalla();
            }
        });

        // Screen no expone properties individuales para sus métricas; cuando
        // cambia la configuración de monitores JavaFX actualiza su lista
        // observable. Escuchamos también ese camino, que en Windows suele
        // llegar antes que el evento del propio Stage.
        Screen.getScreens().addListener((ListChangeListener<Screen>) change -> {
            marcarRecentradoTrasCambioDpi();
            invalidarMetricasPantalla();
            Platform.runLater(this::verificarCambiosPantalla);
        });

        // Al volver a la aplicación después de cambiar la configuración de
        // pantalla hacemos otra comprobación inmediata.
        stage.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                invalidarMetricasPantalla();
                verificarCambiosPantalla();
            }
        });
    }

    /**
     * JavaFX/Windows puede demorar la actualización de outputScale del Stage
     * hasta que la ventana recibe algún evento nativo (por ejemplo, moverla).
     * Por eso observamos directamente las métricas de Screen cuatro veces por
     * segundo. IMPORTANTE: este monitor es sólo de detección. En estado estable
     * no provoca layout, sizeToScene ni movimiento de la ventana.
     */
    private void iniciarMonitorPantalla() {
        monitorPantalla = new Timeline(
                new KeyFrame(Duration.millis(250), event -> verificarCambiosPantalla())
        );
        monitorPantalla.setCycleCount(Animation.INDEFINITE);
        monitorPantalla.play();
    }

    private void verificarCambiosPantalla() {
        if (stage == null || responsiveRoot == null || !stage.isShowing()) {
            return;
        }

        Screen screen = obtenerPantallaActual();
        Rectangle2D visual = screen.getVisualBounds();

        double escalaPantallaX = screen.getOutputScaleX();
        double escalaPantallaY = screen.getOutputScaleY();
        double escalaStageX = stage.getOutputScaleX();
        double escalaStageY = stage.getOutputScaleY();

        boolean metricasPreviasValidas =
                Double.isFinite(ultimaVisualWidth) &&
                Double.isFinite(ultimaVisualHeight) &&
                Double.isFinite(ultimaEscalaPantallaX) &&
                Double.isFinite(ultimaEscalaPantallaY) &&
                Double.isFinite(ultimaEscalaStageX) &&
                Double.isFinite(ultimaEscalaStageY);

        boolean cambioAreaODpi = metricasPreviasValidas && (
                distinto(visual.getMinX(), ultimaVisualX) ||
                distinto(visual.getMinY(), ultimaVisualY) ||
                distinto(visual.getWidth(), ultimaVisualWidth) ||
                distinto(visual.getHeight(), ultimaVisualHeight) ||
                distinto(escalaPantallaX, ultimaEscalaPantallaX) ||
                distinto(escalaPantallaY, ultimaEscalaPantallaY) ||
                distinto(escalaStageX, ultimaEscalaStageX) ||
                distinto(escalaStageY, ultimaEscalaStageY)
        );

        if (cambioAreaODpi) {
            marcarRecentradoTrasCambioDpi();
        }

        boolean cambioMetricas =
                distinto(visual.getMinX(), ultimaVisualX) ||
                distinto(visual.getMinY(), ultimaVisualY) ||
                distinto(visual.getWidth(), ultimaVisualWidth) ||
                distinto(visual.getHeight(), ultimaVisualHeight) ||
                distinto(escalaPantallaX, ultimaEscalaPantallaX) ||
                distinto(escalaPantallaY, ultimaEscalaPantallaY) ||
                distinto(escalaStageX, ultimaEscalaStageX) ||
                distinto(escalaStageY, ultimaEscalaStageY);

        /*
         * No alcanza con mirar si Screen cambió. En Windows el cambio de DPI
         * se propaga de forma asíncrona y puede ocurrir que las métricas ya
         * estén estables pero el tamaño nativo del Stage haya quedado con el
         * DPI anterior. En ese caso la v28 dejaba de corregir porque ya no
         * veía otro cambio de Screen.
         *
         * Por eso verificamos también, en cada pasada del monitor, que el
         * Scene tenga realmente el tamaño que corresponde a la escala actual
         * y que la ventana completa siga dentro del área útil.
         */
        boolean geometriaIncorrecta = necesitaReajusteGeometria(screen);

        if (!cambioMetricas && !geometriaIncorrecta) {
            return;
        }

        ultimaVisualX = visual.getMinX();
        ultimaVisualY = visual.getMinY();
        ultimaVisualWidth = visual.getWidth();
        ultimaVisualHeight = visual.getHeight();
        ultimaEscalaPantallaX = escalaPantallaX;
        ultimaEscalaPantallaY = escalaPantallaY;
        ultimaEscalaStageX = escalaStageX;
        ultimaEscalaStageY = escalaStageY;

        // Primera pasada ya: el usuario no debería tener que tocar la ventana.
        ajustarVentanaAlAreaDisponible(false);

        // Segunda pasada breve para dejar que Windows termine de estabilizar
        // las métricas de barra de título/bordes tras el cambio de DPI.
        programarReajuste();
    }

    private boolean distinto(double a, double b) {
        return !Double.isFinite(b) || Math.abs(a - b) > 0.01;
    }

    private void invalidarMetricasPantalla() {
        ultimaVisualX = Double.NaN;
        ultimaVisualY = Double.NaN;
        ultimaVisualWidth = Double.NaN;
        ultimaVisualHeight = Double.NaN;
        ultimaEscalaPantallaX = Double.NaN;
        ultimaEscalaPantallaY = Double.NaN;
        ultimaEscalaStageX = Double.NaN;
        ultimaEscalaStageY = Double.NaN;
    }

    private void programarReajuste() {
        if (reajustePendiente != null) {
            reajustePendiente.playFromStart();
        }
    }

    private void marcarRecentradoTrasCambioDpi() {
        pasadasRecentradoPendientes = RECENTER_PASSES_AFTER_DPI_CHANGE;
    }

    private boolean hayRecentradoPendiente() {
        return pasadasRecentradoPendientes > 0;
    }

    private void ajustarVentanaAlAreaDisponible(boolean centrarInicialmente) {
        if (stage == null || scene == null || responsiveRoot == null || !stage.isShowing()) {
            return;
        }
        if (ajustandoVentana) {
            return;
        }

        ajustandoVentana = true;
        try {
            Screen screen = obtenerPantallaActual();
            Rectangle2D visualBounds = screen.getVisualBounds();

            double chromeWidthStage = medirChrome(
                    stage.getWidth() - scene.getWidth(),
                    INITIAL_CHROME_WIDTH,
                    120.0
            );
            double chromeHeightStage = medirChrome(
                    stage.getHeight() - scene.getHeight(),
                    INITIAL_CHROME_HEIGHT,
                    160.0
            );

            double chromeWidthScreen = convertirStageAScreen(
                    chromeWidthStage, stage.getOutputScaleX(), screen.getOutputScaleX()
            );
            double chromeHeightScreen = convertirStageAScreen(
                    chromeHeightStage, stage.getOutputScaleY(), screen.getOutputScaleY()
            );

            double visualScale = calcularEscala(visualBounds, chromeWidthScreen, chromeHeightScreen);
            double scale = visualScale * calcularCompensacionDpi(screen);

            double centerX = stage.getX() + stage.getWidth() / 2.0;
            double centerY = stage.getY() + stage.getHeight() / 2.0;

            aplicarEscalaYDimensionar(scale);

            if (!escalasDpiSincronizadas(screen)) {
                // No usamos centerOnScreen() durante una transición de DPI:
                // Windows/JavaFX pueden estar mezclando coordenadas del DPI
                // viejo con las del nuevo y el resultado puede quedar fuera
                // de pantalla. Centramos nosotros mismos usando el tamaño del
                // Stage convertido al sistema de coordenadas del Screen.
                centrarDentroDelArea(screen);
                primeraApertura = false;
            } else if (centrarInicialmente || primeraApertura || hayRecentradoPendiente()) {
                centrarDentroDelArea(screen);
                primeraApertura = false;
            } else {
                reposicionarDentroDelArea(visualBounds, centerX, centerY);
            }
        } finally {
            ajustandoVentana = false;
        }

        /*
         * Importante para cambios 150% -> 100% (y equivalentes): Windows puede
         * aplicar el nuevo outputScale DESPUÉS de nuestro primer sizeToScene().
         * Hacemos otra pasada en el siguiente pulso de JavaFX y volvemos a
         * dimensionar Y reposicionar. En v28 esta segunda pasada podía cambiar
         * el tamaño sin volver a limitar la posición de la ventana.
         */
        Platform.runLater(this::estabilizarVentanaTrasCambioDpi);
    }

    private void estabilizarVentanaTrasCambioDpi() {
        if (stage == null || scene == null || responsiveRoot == null || !stage.isShowing()) {
            return;
        }
        if (ajustandoVentana) {
            return;
        }

        ajustandoVentana = true;
        try {
            Screen screen = obtenerPantallaActual();
            Rectangle2D visualBounds = screen.getVisualBounds();

            double centerX = stage.getX() + stage.getWidth() / 2.0;
            double centerY = stage.getY() + stage.getHeight() / 2.0;

            double chromeWidthStage = medirChrome(
                    stage.getWidth() - scene.getWidth(),
                    INITIAL_CHROME_WIDTH,
                    120.0
            );
            double chromeHeightStage = medirChrome(
                    stage.getHeight() - scene.getHeight(),
                    INITIAL_CHROME_HEIGHT,
                    160.0
            );

            double chromeWidthScreen = convertirStageAScreen(
                    chromeWidthStage, stage.getOutputScaleX(), screen.getOutputScaleX()
            );
            double chromeHeightScreen = convertirStageAScreen(
                    chromeHeightStage, stage.getOutputScaleY(), screen.getOutputScaleY()
            );

            double visualScale = calcularEscala(visualBounds, chromeWidthScreen, chromeHeightScreen);
            double exactScale = visualScale * calcularCompensacionDpi(screen);
            aplicarEscalaYDimensionar(exactScale);

            if (!escalasDpiSincronizadas(screen)) {
                centrarDentroDelArea(screen);
                intentarForzarActualizacionDpi(screen);
            } else if (hayRecentradoPendiente()) {
                /*
                 * Este es el caso que corrige V31: al pasar, por ejemplo,
                 * 100% -> 150%, el tamaño puede quedar perfecto pero las
                 * coordenadas heredadas del DPI anterior dejan la barra de
                 * título fuera de pantalla. Cuando ambas escalas ya coinciden
                 * recentramos explícitamente dentro del visualBounds actual.
                 */
                centrarDentroDelArea(screen);
            } else {
                reposicionarDentroDelArea(visualBounds, centerX, centerY);
            }
        } finally {
            ajustandoVentana = false;
        }

        if (pasadasRecentradoPendientes > 0) {
            pasadasRecentradoPendientes--;
            if (pasadasRecentradoPendientes > 0) {
                programarReajuste();
            }
        }
    }

    private void aplicarEscalaYDimensionar(double scale) {
        double expectedSceneWidth = DESIGN_WIDTH * scale;
        double expectedSceneHeight = DESIGN_HEIGHT * scale;

        boolean cambioEscala =
                Math.abs(responsiveRoot.getScaleFactor() - scale) > SCALE_APPLY_TOLERANCE;
        boolean cambioTamano =
                Math.abs(scene.getWidth() - expectedSceneWidth) > SCENE_SIZE_TOLERANCE ||
                Math.abs(scene.getHeight() - expectedSceneHeight) > SCENE_SIZE_TOLERANCE;

        /*
         * V32 recalculaba layout + sizeToScene en cada pasada del monitor si
         * Stage y Screen mantenían una pequeña discrepancia de DPI. Aunque la
         * geometría final ya fuese correcta, ese trabajo repetido se percibía
         * como una "vibración" de la interfaz.
         *
         * En V33, si la escala y el tamaño visibles ya son correctos, esta
         * función es deliberadamente un no-op. El monitor puede seguir leyendo
         * las métricas cada 250 ms sin redibujar nada.
         */
        if (!cambioEscala && !cambioTamano) {
            return;
        }

        responsiveRoot.setScaleFactor(scale);
        responsiveRoot.applyCss();
        responsiveRoot.layout();

        // Sólo redimensionamos el Stage cuando realmente cambió la geometría.
        stage.sizeToScene();
    }

    /**
     * Centra el Stage dentro del área útil incluso mientras Stage y Screen
     * reportan escalas DPI distintas. Durante esa transición stage.getWidth()
     * y stage.getHeight() todavía pueden estar expresados con la escala vieja;
     * por eso convertimos primero esas dimensiones a coordenadas del Screen.
     */
    private void centrarDentroDelArea(Screen screen) {
        Rectangle2D visualBounds = screen.getVisualBounds();

        double windowWidthScreen = convertirStageAScreen(
                stage.getWidth(), stage.getOutputScaleX(), screen.getOutputScaleX()
        );
        double windowHeightScreen = convertirStageAScreen(
                stage.getHeight(), stage.getOutputScaleY(), screen.getOutputScaleY()
        );

        if (!Double.isFinite(windowWidthScreen) || windowWidthScreen <= 0.0) {
            windowWidthScreen = stage.getWidth();
        }
        if (!Double.isFinite(windowHeightScreen) || windowHeightScreen <= 0.0) {
            windowHeightScreen = stage.getHeight();
        }

        double newX = visualBounds.getMinX() +
                (visualBounds.getWidth() - windowWidthScreen) / 2.0;
        double newY = visualBounds.getMinY() +
                (visualBounds.getHeight() - windowHeightScreen) / 2.0;

        newX = limitar(
                newX,
                visualBounds.getMinX(),
                visualBounds.getMaxX() - windowWidthScreen
        );
        newY = limitar(
                newY,
                visualBounds.getMinY(),
                visualBounds.getMaxY() - windowHeightScreen
        );

        stage.setX(newX);
        stage.setY(newY);
    }

    private void reposicionarDentroDelArea(Rectangle2D visualBounds,
                                           double centerX,
                                           double centerY) {
        double newX = centerX - stage.getWidth() / 2.0;
        double newY = centerY - stage.getHeight() / 2.0;

        newX = limitar(
                newX,
                visualBounds.getMinX(),
                visualBounds.getMaxX() - stage.getWidth()
        );
        newY = limitar(
                newY,
                visualBounds.getMinY(),
                visualBounds.getMaxY() - stage.getHeight()
        );

        stage.setX(newX);
        stage.setY(newY);
    }

    /**
     * Verifica no sólo cambios de Screen/DPI, sino también si el Stage quedó
     * con un tamaño lógico viejo después de que Windows terminó la transición.
     * Esto hace al monitor autocorrectivo aunque no llegue un nuevo evento.
     */
    private boolean necesitaReajusteGeometria(Screen screen) {
        if (scene == null || responsiveRoot == null) {
            return false;
        }

        Rectangle2D visualBounds = screen.getVisualBounds();

        double chromeWidthStage = medirChrome(
                stage.getWidth() - scene.getWidth(),
                INITIAL_CHROME_WIDTH,
                120.0
        );
        double chromeHeightStage = medirChrome(
                stage.getHeight() - scene.getHeight(),
                INITIAL_CHROME_HEIGHT,
                160.0
        );

        double chromeWidthScreen = convertirStageAScreen(
                chromeWidthStage, stage.getOutputScaleX(), screen.getOutputScaleX()
        );
        double chromeHeightScreen = convertirStageAScreen(
                chromeHeightStage, stage.getOutputScaleY(), screen.getOutputScaleY()
        );

        double visualScale = calcularEscala(visualBounds, chromeWidthScreen, chromeHeightScreen);
        double expectedScale = visualScale * calcularCompensacionDpi(screen);
        double expectedSceneWidth = DESIGN_WIDTH * expectedScale;
        double expectedSceneHeight = DESIGN_HEIGHT * expectedScale;

        boolean sceneIncorrecta =
                Math.abs(scene.getWidth() - expectedSceneWidth) > 2.0 ||
                Math.abs(scene.getHeight() - expectedSceneHeight) > 2.0 ||
                Math.abs(responsiveRoot.getScaleFactor() - expectedScale) > 0.002;

        /*
         * Si Stage y Screen todavía tienen DPI distintos, la geometría reportada
         * por Stage está expresada con la escala vieja. Convertimos su tamaño a
         * coordenadas del Screen actual antes de decidir si está fuera de área.
         */
        double stageWidthScreen = convertirStageAScreen(
                stage.getWidth(), stage.getOutputScaleX(), screen.getOutputScaleX()
        );
        double stageHeightScreen = convertirStageAScreen(
                stage.getHeight(), stage.getOutputScaleY(), screen.getOutputScaleY()
        );

        final double tolerance = 2.0;
        boolean fueraDelArea =
                stage.getX() < visualBounds.getMinX() - tolerance ||
                stage.getY() < visualBounds.getMinY() - tolerance ||
                stage.getX() + stageWidthScreen > visualBounds.getMaxX() + tolerance ||
                stage.getY() + stageHeightScreen > visualBounds.getMaxY() + tolerance;

        /*
         * Una diferencia persistente entre outputScale de Screen y Stage NO es,
         * por sí sola, un error de geometría. La compensación DPI ya mantiene el
         * tamaño físico correcto durante esa situación.
         *
         * En V32 esa diferencia hacía que este método devolviera true para
         * siempre en algunos equipos con 125/150/175%, por lo que el monitor de
         * 250 ms volvía a ejecutar layout/sizeToScene indefinidamente. Eso era
         * la causa de la vibración perceptible.
         */
        return sceneIncorrecta || fueraDelArea;
    }

    /**
     * Devuelve la corrección necesaria mientras el Stage conserva el DPI
     * anterior. Ejemplo: Screen ya está en 100% (1.0) pero Stage sigue en
     * 150% (1.5): usamos 1.0 / 1.5 = 0.666..., de modo que el tamaño físico
     * visible siga siendo exactamente el deseado y no se agrande 1.5 veces.
     */
    private double calcularCompensacionDpi(Screen screen) {
        double ratioX = ratioSeguro(screen.getOutputScaleX(), stage.getOutputScaleX());
        double ratioY = ratioSeguro(screen.getOutputScaleY(), stage.getOutputScaleY());
        return Math.min(ratioX, ratioY);
    }

    private double ratioSeguro(double screenScale, double stageScale) {
        if (!Double.isFinite(screenScale) || !Double.isFinite(stageScale) ||
                screenScale <= 0.0 || stageScale <= 0.0) {
            return 1.0;
        }
        return screenScale / stageScale;
    }

    private double convertirStageAScreen(double value, double stageScale, double screenScale) {
        if (!Double.isFinite(value)) {
            return value;
        }
        if (!Double.isFinite(stageScale) || !Double.isFinite(screenScale) ||
                stageScale <= 0.0 || screenScale <= 0.0) {
            return value;
        }
        return value * stageScale / screenScale;
    }

    private boolean escalasDpiSincronizadas(Screen screen) {
        return Math.abs(screen.getOutputScaleX() - stage.getOutputScaleX()) <= DPI_TOLERANCE &&
               Math.abs(screen.getOutputScaleY() - stage.getOutputScaleY()) <= DPI_TOLERANCE;
    }

    /**
     * En Windows, mover mínimamente una ventana hace que el sistema entregue
     * al Stage el WM_DPICHANGED pendiente. Lo hacemos de forma automática y
     * prácticamente imperceptible cuando Screen y Stage no coinciden.
     */
    private void intentarForzarActualizacionDpi(Screen screen) {
        if (forzandoActualizacionDpi || escalasDpiSincronizadas(screen)) {
            return;
        }

        long ahora = System.nanoTime();
        if (ahora - ultimoForzadoDpiNanos < DPI_NUDGE_COOLDOWN_NANOS) {
            return;
        }
        ultimoForzadoDpiNanos = ahora;

        Rectangle2D visual = screen.getVisualBounds();

        // Partimos siempre de una posición segura. En V30/V31 el pequeño
        // movimiento se revertía a la coordenada anterior; si esa coordenada
        // había quedado fuera de pantalla por el cambio de DPI, terminábamos
        // reintroduciendo exactamente el problema que queríamos corregir.
        centrarDentroDelArea(screen);
        double baseX = stage.getX();

        double desplazadoX = baseX + 2.0;
        if (desplazadoX > visual.getMaxX() - 4.0) {
            desplazadoX = baseX - 2.0;
        }

        final double xTemporal = desplazadoX;
        forzandoActualizacionDpi = true;
        stage.setX(xTemporal);

        Platform.runLater(() -> {
            try {
                // No restauramos una coordenada histórica. Volvemos a
                // calcular el centro con las métricas que JavaFX tenga en este
                // nuevo pulso, ya sean todavía las viejas o ya las definitivas.
                Screen currentScreen = obtenerPantallaActual();
                centrarDentroDelArea(currentScreen);
            } finally {
                forzandoActualizacionDpi = false;
                invalidarMetricasPantalla();
                marcarRecentradoTrasCambioDpi();
                programarReajuste();
            }
        });
    }

    private Screen obtenerPantallaActual() {
        List<Screen> screens = Screen.getScreensForRectangle(
                stage.getX(),
                stage.getY(),
                Math.max(1.0, stage.getWidth()),
                Math.max(1.0, stage.getHeight())
        );

        if (screens == null || screens.isEmpty()) {
            return Screen.getPrimary();
        }
        return screens.get(0);
    }

    private double medirChrome(double value, double fallback, double maxReasonable) {
        if (!Double.isFinite(value) || value < 0.0 || value > maxReasonable) {
            return fallback;
        }
        return value;
    }

    /**
     * Calcula una escala uniforme para que la ventana completa entre dentro del
     * área visual (barra de tareas/dock ya descontados por getVisualBounds()).
     * Nunca agranda la interfaz por encima del tamaño original.
     */
    private double calcularEscala(Rectangle2D visualBounds,
                                  double chromeWidth,
                                  double chromeHeight) {
        double availableWidth = Math.max(
                1.0,
                visualBounds.getWidth() - chromeWidth - SAFETY_MARGIN
        );
        double availableHeight = Math.max(
                1.0,
                visualBounds.getHeight() - chromeHeight - SAFETY_MARGIN
        );

        double scaleX = availableWidth / DESIGN_WIDTH;
        double scaleY = availableHeight / DESIGN_HEIGHT;

        return Math.min(1.0, Math.min(scaleX, scaleY));
    }

    private double limitar(double value, double min, double max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Mantiene al FXML SIEMPRE maquetado en sus 480x785 originales y sólo
     * escala su representación visual. De esta forma no se deforman las
     * proporciones internas ni se acumulan cambios al pasar 100% -> 150% ->
     * 100%.
     */
    private static final class ResponsivePane extends Pane {
        private final Parent content;
        private final Scale scaleTransform;
        private double scaleFactor;

        private ResponsivePane(Parent content, double initialScale) {
            this.content = content;
            this.content.setManaged(false);

            this.scaleTransform = new Scale(1.0, 1.0, 0.0, 0.0);
            this.content.getTransforms().add(scaleTransform);
            getChildren().add(content);

            setScaleFactor(initialScale);
        }

        private double getScaleFactor() {
            return scaleFactor;
        }

        private void setScaleFactor(double scale) {
            /*
             * Normalmente la escala visual nunca supera 1.0. Durante los pocos
             * milisegundos en que Screen ya cambió de DPI pero Stage conserva
             * el anterior, puede ser necesario un valor lógico > 1.0 para que
             * el tamaño físico final siga siendo el mismo. Por eso permitimos
             * una compensación temporal amplia, sin afectar el tamaño efectivo.
             */
            double safeScale = Math.max(0.10, Math.min(3.0, scale));
            this.scaleFactor = safeScale;

            scaleTransform.setX(safeScale);
            scaleTransform.setY(safeScale);

            double width = DESIGN_WIDTH * safeScale;
            double height = DESIGN_HEIGHT * safeScale;

            setMinSize(width, height);
            setPrefSize(width, height);
            setMaxSize(width, height);
            resize(width, height);
            requestLayout();
        }

        @Override
        protected void layoutChildren() {
            if (content.isResizable()) {
                content.resize(DESIGN_WIDTH, DESIGN_HEIGHT);
            }
            content.relocate(0.0, 0.0);
            content.applyCss();
            content.layout();
        }
    }

    @Override
    public void stop() {
        if (monitorPantalla != null) {
            monitorPantalla.stop();
        }
        if (reajustePendiente != null) {
            reajustePendiente.stop();
        }
        DatabaseConnection.closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
