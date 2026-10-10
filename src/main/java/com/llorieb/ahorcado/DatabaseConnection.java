package com.llorieb.ahorcado;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Opens the SQLite database from a writable user-data directory.
 *
 * Resources inside a packaged JAR/runtime image are not normal files, so the
 * bundled database is copied to the user-data directory the first time the
 * game runs. Existing local databases are migrated in place without deleting
 * custom words added by the user.
 */
public final class DatabaseConnection {
    private static final String DATABASE_RESOURCE = "/ahorcado.db";
    private static final String CORPUS_RESOURCE = "/palabras_v22.tsv";
    private static final String DATABASE_FILE_NAME = "ahorcado.db";
    private static final int DATABASE_VERSION = 6;
    private static Connection connection;

    private DatabaseConnection() {
    }

    public static synchronized Connection connect() {
        try {
            if (connection == null || connection.isClosed()) {
                Path databasePath = prepareDatabase();
                connection = DriverManager.getConnection(
                        "jdbc:sqlite:" + databasePath.toAbsolutePath()
                );
            }
            return connection;
        } catch (IOException | SQLException e) {
            throw new IllegalStateException(
                    "No se pudo abrir la base de datos del juego.",
                    e
            );
        }
    }

    private static Path prepareDatabase() throws IOException {
        Path appDirectory = getApplicationDataDirectory();
        Files.createDirectories(appDirectory);

        Path databasePath = appDirectory.resolve(DATABASE_FILE_NAME);

        if (Files.notExists(databasePath)) {
            copyBundledDatabase(databasePath);
        } else {
            upgradeDatabase(databasePath);
        }

        return databasePath;
    }

    /**
     * Versión 6:
     * - conserva las correcciones y el corpus de versiones anteriores;
     * - incorpora las categorías Equipos de fútbol y Películas;
     * - normaliza los títulos de películas según su denominación de estreno/uso
     *   habitual en Argentina;
     * - mantiene el índice único para impedir duplicados futuros.
     *
     * Las palabras personalizadas del usuario se conservan. Al actualizar desde
     * una instalación anterior o desde una build de prueba de 1.1.0, el corpus
     * se completa y los títulos históricos se corrigen sin borrar la base local.
     */
    private static void upgradeDatabase(Path databasePath) throws IOException {
        try (Connection localConnection = DriverManager.getConnection(
                "jdbc:sqlite:" + databasePath.toAbsolutePath()
        )) {
            int localVersion;

            try (var statement = localConnection.createStatement();
                 var resultSet = statement.executeQuery("PRAGMA user_version")) {
                localVersion = resultSet.next() ? resultSet.getInt(1) : 0;
            }

            if (localVersion >= DATABASE_VERSION) {
                return;
            }

            boolean autoCommitOriginal = localConnection.getAutoCommit();
            localConnection.setAutoCommit(false);

            try {
                normalizarDatosHistoricos(localConnection);
                eliminarDuplicados(localConnection);
                cargarCorpusCompleto(localConnection);
                eliminarDuplicados(localConnection);
                crearIndiceUnico(localConnection);

                try (var statement = localConnection.createStatement()) {
                    statement.execute("PRAGMA user_version = " + DATABASE_VERSION);
                }

                localConnection.commit();
            } catch (IOException | SQLException e) {
                localConnection.rollback();
                throw e;
            } finally {
                localConnection.setAutoCommit(autoCommitOriginal);
            }

        } catch (SQLException e) {
            throw new IOException(
                    "No se pudo actualizar la base de datos local.",
                    e
            );
        }
    }

    private static void normalizarDatosHistoricos(
            Connection connection
    ) throws SQLException {
        // Correcciones de la base incluida en versiones anteriores.
        actualizarNombreHistorico(connection, "Paises", "EL SAVADOR", "EL SALVADOR");
        actualizarNombreHistorico(connection, "Paises", "PANAMA", "PANAMÁ");
        actualizarNombreHistorico(connection, "Paises", "PERU", "PERÚ");
        actualizarNombreHistorico(
                connection,
                "Paises",
                "EMIRATOS ÁRABES",
                "EMIRATOS ÁRABES UNIDOS"
        );

        actualizarNombreHistorico(connection, "Ciudades", "PARIS", "PARÍS");
        actualizarNombreHistorico(connection, "Ciudades", "SAN PABLO", "SÃO PAULO");

        actualizarNombreHistorico(
                connection,
                "Marcas de autos",
                "MERCEDES BENZ",
                "MERCEDES-BENZ"
        );

        // Títulos de películas normalizados al nombre de estreno/uso en Argentina.
        actualizarNombreHistorico(connection, "Peliculas", "ALIEN", "ALIEN EL OCTAVO PASAJERO");
        actualizarNombreHistorico(connection, "Peliculas", "ALIENS", "ALIENS EL REGRESO");
        actualizarNombreHistorico(connection, "Peliculas", "BLACK PANTHER", "PANTERA NEGRA");
        actualizarNombreHistorico(connection, "Peliculas", "CAPITÁN AMÉRICA", "CAPITÁN AMÉRICA EL PRIMER VENGADOR");
        actualizarNombreHistorico(connection, "Peliculas", "CON FALDAS Y A LO LOCO", "UNA EVA Y DOS ADANES");
        actualizarNombreHistorico(connection, "Peliculas", "CREED", "CREED CORAZÓN DE CAMPEÓN");
        actualizarNombreHistorico(connection, "Peliculas", "EL CABALLERO OSCURO", "BATMAN EL CABALLERO DE LA NOCHE");
        actualizarNombreHistorico(connection, "Peliculas", "EL HOBBIT", "EL HOBBIT UN VIAJE INESPERADO");
        actualizarNombreHistorico(connection, "Peliculas", "EL PRESTIGIO", "EL GRAN TRUCO");
        actualizarNombreHistorico(connection, "Peliculas", "ACE VENTURA", "ACE VENTURA DETECTIVE DE MASCOTAS");
        actualizarNombreHistorico(connection, "Peliculas", "EL PROYECTO DE LA BRUJA DE BLAIR", "EL PROYECTO BLAIR WITCH");
        actualizarNombreHistorico(connection, "Peliculas", "PULP FICTION", "TIEMPOS VIOLENTOS");
        actualizarNombreHistorico(connection, "Peliculas", "SEVEN", "SEVEN LOS SIETE PECADOS CAPITALES");
        actualizarNombreHistorico(connection, "Peliculas", "EL PRISIONERO DE AZKABAN", "HARRY POTTER Y EL PRISIONERO DE AZKABAN");
        actualizarNombreHistorico(connection, "Peliculas", "EL RETORNO DEL REY", "EL SEÑOR DE LOS ANILLOS EL RETORNO DEL REY");
        actualizarNombreHistorico(connection, "Peliculas", "EL SEÑOR DE LOS ANILLOS", "EL SEÑOR DE LOS ANILLOS LA COMUNIDAD DEL ANILLO");
        actualizarNombreHistorico(connection, "Peliculas", "ENDGAME", "AVENGERS ENDGAME");
        actualizarNombreHistorico(connection, "Peliculas", "ET", "E.T. EL EXTRATERRESTRE");
        actualizarNombreHistorico(connection, "Peliculas", "FURIA EN EL CAMINO", "MAD MAX FURIA EN EL CAMINO");
        actualizarNombreHistorico(connection, "Peliculas", "GHOST", "GHOST LA SOMBRA DEL AMOR");
        actualizarNombreHistorico(connection, "Peliculas", "GUERRA INFINITA", "AVENGERS INFINITY WAR");
        actualizarNombreHistorico(connection, "Peliculas", "HARRY POTTER", "HARRY POTTER Y EL CÁLIZ DE FUEGO");
        actualizarNombreHistorico(connection, "Peliculas", "HEAT", "FUEGO CONTRA FUEGO");
        actualizarNombreHistorico(connection, "Peliculas", "HEREDITARY", "EL LEGADO DEL DIABLO");
        actualizarNombreHistorico(connection, "Peliculas", "HOMBRE DE HIERRO", "IRON MAN");
        actualizarNombreHistorico(connection, "Peliculas", "INSIDIOUS", "LA NOCHE DEL DEMONIO");
        actualizarNombreHistorico(connection, "Peliculas", "INTENSA MENTE", "INTENSAMENTE");
        actualizarNombreHistorico(connection, "Peliculas", "JOHN WICK", "SIN CONTROL JOHN WICK");
        actualizarNombreHistorico(connection, "Peliculas", "LA CÁMARA SECRETA", "HARRY POTTER Y LA CÁMARA SECRETA");
        actualizarNombreHistorico(connection, "Peliculas", "LA PIEDRA FILOSOFAL", "HARRY POTTER Y LA PIEDRA FILOSOFAL");
        actualizarNombreHistorico(connection, "Peliculas", "LAS DOS TORRES", "EL SEÑOR DE LOS ANILLOS LAS DOS TORRES");
        actualizarNombreHistorico(connection, "Peliculas", "MARCIANO", "MISIÓN RESCATE");
        actualizarNombreHistorico(connection, "Peliculas", "NOTTING HILL", "UN LUGAR LLAMADO NOTTING HILL");
        actualizarNombreHistorico(connection, "Peliculas", "PIRATAS DEL CARIBE", "PIRATAS DEL CARIBE LA MALDICIÓN DEL PERLA NEGRA");
        actualizarNombreHistorico(connection, "Peliculas", "PRISIONEROS", "LA SOSPECHA");
        actualizarNombreHistorico(connection, "Peliculas", "TERMINATOR 2", "TERMINATOR 2 EL JUICIO FINAL");
        actualizarNombreHistorico(connection, "Peliculas", "TODO PODEROSO", "TODOPODEROSO");
        actualizarNombreHistorico(connection, "Peliculas", "UP", "UP UNA AVENTURA DE ALTURA");
    }

    private static void actualizarNombreHistorico(
            Connection connection,
            String categoria,
            String anterior,
            String nuevo
    ) throws SQLException {
        String sql = "UPDATE palabras SET palabra = ? "
                + "WHERE desc_categoria COLLATE NOCASE = ? "
                + "AND palabra COLLATE NOCASE = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nuevo);
            statement.setString(2, categoria);
            statement.setString(3, anterior);
            statement.executeUpdate();
        }
    }

    private static void cargarCorpusCompleto(
            Connection connection
    ) throws IOException, SQLException {
        InputStream input = DatabaseConnection.class.getResourceAsStream(
                CORPUS_RESOURCE
        );

        if (input == null) {
            throw new IOException(
                    "No se encontró el recurso " + CORPUS_RESOURCE
            );
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8)
        )) {
            String line;
            int numeroLinea = 0;

            while ((line = reader.readLine()) != null) {
                numeroLinea++;
                String limpia = line.trim();

                if (limpia.isEmpty() || limpia.startsWith("#")) {
                    continue;
                }

                String[] partes = line.split("\t", 3);
                if (partes.length != 3) {
                    throw new IOException(
                            "Línea inválida en " + CORPUS_RESOURCE
                                    + ": " + numeroLinea
                    );
                }

                int idCategoria;
                try {
                    idCategoria = Integer.parseInt(partes[0].trim());
                } catch (NumberFormatException e) {
                    throw new IOException(
                            "Categoría inválida en " + CORPUS_RESOURCE
                                    + ": " + numeroLinea,
                            e
                    );
                }

                String categoria = partes[1].trim();
                String palabra = partes[2].trim();

                insertarSiFalta(
                        connection,
                        idCategoria,
                        categoria,
                        palabra
                );
            }
        }
    }

    private static void eliminarDuplicados(
            Connection connection
    ) throws SQLException {
        String sql = "DELETE FROM palabras "
                + "WHERE rowid NOT IN ("
                + "SELECT MIN(rowid) FROM palabras "
                + "GROUP BY desc_categoria COLLATE NOCASE, "
                + "palabra COLLATE NOCASE"
                + ")";

        try (var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void crearIndiceUnico(
            Connection connection
    ) throws SQLException {
        String sql = "CREATE UNIQUE INDEX IF NOT EXISTS "
                + "uq_palabras_categoria_palabra "
                + "ON palabras("
                + "desc_categoria COLLATE NOCASE, "
                + "palabra COLLATE NOCASE"
                + ")";

        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static void insertarSiFalta(
            Connection connection,
            int idCategoria,
            String descripcionCategoria,
            String palabra
    ) throws SQLException {
        String sql = "INSERT INTO palabras "
                + "(id_categoria, desc_categoria, palabra) "
                + "SELECT ?, ?, ? "
                + "WHERE NOT EXISTS ("
                + "SELECT 1 FROM palabras "
                + "WHERE desc_categoria COLLATE NOCASE = ? "
                + "AND palabra COLLATE NOCASE = ?"
                + ")";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idCategoria);
            statement.setString(2, descripcionCategoria);
            statement.setString(3, palabra);
            statement.setString(4, descripcionCategoria);
            statement.setString(5, palabra);
            statement.executeUpdate();
        }
    }

    private static void copyBundledDatabase(
            Path databasePath
    ) throws IOException {
        try (InputStream input = DatabaseConnection.class.getResourceAsStream(
                DATABASE_RESOURCE
        )) {
            if (input == null) {
                throw new IOException(
                        "No se encontró el recurso " + DATABASE_RESOURCE
                );
            }

            Files.copy(
                    input,
                    databasePath,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private static Path getApplicationDataDirectory() {
        String localAppData = System.getenv("LOCALAPPDATA");

        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "Ahorcado");
        }

        return Path.of(
                System.getProperty("user.home"),
                ".ahorcado"
        );
    }

    public static synchronized void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            connection = null;
        }
    }
}
