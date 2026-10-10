package com.llorieb.ahorcado;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Prueba manual de estrés de 2000 partidas. Se omite en builds normales.
 *
 * Ejecutar con:
 *   mvnw.cmd -Dahorcado.stress=true -Dtest=PartidasStressTest test
 * o en Linux:
 *   ./mvnw -Dahorcado.stress=true -Dtest=PartidasStressTest test
 */
class PartidasStressTest {
    private static final int PARTIDAS_STRESS = 2000;
    private static final int INTERVALO_REPORTE = 250;
    private static boolean stressActivo;

    @BeforeAll
    static void iniciarJavaFx() throws Exception {
        stressActivo = Boolean.getBoolean("ahorcado.stress");
        Assumptions.assumeTrue(stressActivo,
                "Stress test omitido. Usar -Dahorcado.stress=true para ejecutarlo.");

        FutureTask<Void> startup = new FutureTask<>(() -> null);
        try {
            Platform.startup(startup);
            startup.get(10, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyStarted) {
            // El toolkit ya estaba iniciado por otra prueba.
        }
    }

    @AfterAll
    static void cerrarJavaFx() {
        if (stressActivo) {
            DatabaseConnection.closeConnection();
            Platform.exit();
        }
    }

    @Test
    void ejecutarDosMilPartidas() throws Exception {
        ejecutarEnFx(() -> {
            FXMLLoader loader = new FXMLLoader(
                    PartidasStressTest.class.getResource("/AhorcadoLayout.fxml")
            );
            Parent root = loader.load();
            AhorcadoController controller = loader.getController();

            Button jugar = (Button) loader.getNamespace().get("btnJugar");
            Button detener = (Button) loader.getNamespace().get("btnDetener");

            assertNotNull(controller);
            assertNotNull(jugar);
            assertNotNull(detener);

            imprimirMemoria("inicio", 0);

            for (int i = 1; i <= PARTIDAS_STRESS; i++) {
                jugar.fire();
                detener.fire();

                if (i % INTERVALO_REPORTE == 0) {
                    imprimirMemoria("partidas", i);
                }
            }

            controller.liberarRecursos();
            return null;
        });

        // Una medición posterior a GC sirve para detectar tendencias gruesas,
        // no como una aserción rígida: la JVM decide cuándo ampliar/reducir heap.
        System.gc();
        Thread.sleep(500);
        imprimirMemoria("tras GC", PARTIDAS_STRESS);
    }

    private static <T> T ejecutarEnFx(Callable<T> tarea) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return tarea.call();
        }

        FutureTask<T> future = new FutureTask<>(tarea);
        Platform.runLater(future);
        return future.get(60, TimeUnit.SECONDS);
    }

    private static void imprimirMemoria(String etapa, int partidas) {
        Runtime runtime = Runtime.getRuntime();
        long usada = runtime.totalMemory() - runtime.freeMemory();
        long comprometida = runtime.totalMemory();
        long maxima = runtime.maxMemory();

        System.out.printf(
                "[STRESS] %-10s partidas=%3d | usada=%6.1f MB | heap=%6.1f MB | max=%6.1f MB%n",
                etapa,
                partidas,
                usada / 1024.0 / 1024.0,
                comprometida / 1024.0 / 1024.0,
                maxima / 1024.0 / 1024.0
        );
    }
}
