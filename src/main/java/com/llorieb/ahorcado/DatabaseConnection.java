package com.llorieb.ahorcado;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Objects;

public class DatabaseConnection {
    private static Connection connection;

    public static Connection connect() {
        if (connection == null) {
            try {
                // Obtén el ClassLoader del contexto actual
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

                // Carga el archivo de base de datos desde el directorio resources
                String dbPath = Objects.requireNonNull(classLoader.getResource("ahorcado.db")).getFile();

                // Establece la conexión con la base de datos
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
